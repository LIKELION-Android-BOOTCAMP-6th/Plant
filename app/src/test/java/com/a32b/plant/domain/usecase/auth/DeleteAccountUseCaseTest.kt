package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeAuthRepository
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountUseCaseTest {
    @Test
    fun `successful deletion ends session signs out and clears local settings`() = runBlocking {
        val clock = CallClock()
        val users = FakeUserRepository(testUser(), callLog = clock.calls)
        val auth = FakeAuthRepository(callLog = clock.calls)
        val study = FakeStudyingRepository()
        var sessionExpiredEvents = 0
        val useCase = createUseCase(users, auth, study) { sessionExpiredEvents++ }

        assertTrue(useCase() is Result.Success)

        assertEquals(0, sessionExpiredEvents)
        // 토큰 갱신 → 구독 해제 → 함수 호출 → 로컬 정리(구독 해제는 멱등) → 로그아웃 순서
        assertEquals(
            listOf("refreshIdToken", "endUserSession", "deleteAccount", "endUserSession", "signOut"),
            clock.calls
        )
        assertTrue(users.startedSessions.isEmpty())
        assertTrue(users.currentUser.value == null)
        assertEquals(listOf(false), users.darkModeUpdates)
        assertEquals(1, study.clearLocalSessionCalls)
    }

    @Test
    fun `function failure restores session keeps local settings and retry succeeds`() = runBlocking {
        val clock = CallClock()
        val user = testUser()
        val users = FakeUserRepository(user, callLog = clock.calls)
        val auth = FakeAuthRepository(deleteAccountResult = Result.Failure(AppError.Network()), callLog = clock.calls)
        val study = FakeStudyingRepository()
        var sessionExpiredEvents = 0
        val useCase = createUseCase(users, auth, study) { sessionExpiredEvents++ }

        val failed = useCase()

        assertEquals(Result.Failure(AppError.Network()), failed)
        assertEquals(
            listOf("refreshIdToken", "endUserSession", "deleteAccount", "startUserSession"),
            clock.calls
        )
        assertEquals(listOf(user), users.startedSessions)
        assertEquals(user, users.currentUser.value)
        assertEquals(0, auth.signOutCalls)
        assertTrue(users.darkModeUpdates.isEmpty())
        assertEquals(0, study.clearLocalSessionCalls)
        assertEquals(0, sessionExpiredEvents)

        // 재시도: 서버가 남은 단계부터 이어서 처리해 성공
        auth.deleteAccountResult = Result.Success(Unit)
        assertTrue(useCase() is Result.Success)
        assertEquals(2, auth.deleteAccountCalls)
        assertEquals(1, auth.signOutCalls)
        assertTrue(users.currentUser.value == null)
    }

    @Test
    fun `stale reauthentication message from server is passed through`() = runBlocking {
        val clock = CallClock()
        val message = "보안을 위해 Google 계정을 다시 확인한 뒤 시도해주세요."
        val users = FakeUserRepository(testUser(), callLog = clock.calls)
        val auth = FakeAuthRepository(deleteAccountResult = Result.Failure(AppError.Custom(message)), callLog = clock.calls)
        val useCase = createUseCase(users, auth)

        val result = useCase()

        assertEquals(Result.Failure(AppError.Custom(message)), result)
        assertEquals(1, users.startedSessions.size)
        assertEquals(0, auth.signOutCalls)
    }

    @Test
    fun `token refresh failure does not end session or call function`() = runBlocking {
        val clock = CallClock()
        val user = testUser()
        val users = FakeUserRepository(user, callLog = clock.calls)
        val auth = FakeAuthRepository(refreshResult = Result.Failure(AppError.Network()), callLog = clock.calls)
        val useCase = createUseCase(users, auth)

        assertEquals(Result.Failure(AppError.Network()), useCase())

        assertEquals(listOf("refreshIdToken"), clock.calls)
        assertEquals(user, users.currentUser.value)
    }

    @Test
    fun `already deleted auth account is treated as completed deletion`() = runBlocking {
        val clock = CallClock()
        val users = FakeUserRepository(testUser(), callLog = clock.calls)
        val auth = FakeAuthRepository(refreshResult = Result.Failure(AppError.AccountNotFound()), callLog = clock.calls)
        val study = FakeStudyingRepository()
        var sessionExpiredEvents = 0
        val useCase = createUseCase(users, auth, study) { sessionExpiredEvents++ }

        assertTrue(useCase() is Result.Success)

        assertEquals(listOf("refreshIdToken", "endUserSession", "signOut"), clock.calls)
        assertTrue(users.currentUser.value == null)
        assertEquals(listOf(false), users.darkModeUpdates)
        assertEquals(1, study.clearLocalSessionCalls)
        assertEquals(0, sessionExpiredEvents)
    }

    @Test
    fun `cancellation during function call restores session`() = runBlocking {
        val clock = CallClock()
        val user = testUser()
        val users = FakeUserRepository(user, callLog = clock.calls)
        val auth = FakeAuthRepository(onDeleteAccount = { throw CancellationException("test cancellation") }, callLog = clock.calls)
        val useCase = createUseCase(users, auth)

        val cancelled = try {
            useCase()
            false
        } catch (_: CancellationException) {
            true
        }

        assertTrue(cancelled)
        assertEquals(listOf(user), users.startedSessions)
        assertEquals(user, users.currentUser.value)
        assertEquals(0, auth.signOutCalls)
    }

    @Test
    fun `missing current user notifies session expired without calling function`() = runBlocking {
        val clock = CallClock()
        val users = FakeUserRepository(null, callLog = clock.calls)
        val auth = FakeAuthRepository(callLog = clock.calls)
        var sessionExpiredEvents = 0
        val useCase = createUseCase(users, auth) { sessionExpiredEvents++ }

        val result = useCase()

        assertEquals(Result.Failure(AppError.UnknownUser()), result)
        assertEquals(1, sessionExpiredEvents)
        assertTrue(clock.calls.isEmpty())
    }

    private fun createUseCase(
        users: FakeUserRepository,
        auth: AuthRepository,
        study: StudyingRepository = FakeStudyingRepository(),
        onSessionExpired: () -> Unit = {}
    ) = DeleteAccountUseCase(
        authRepository = auth,
        userRepository = users,
        studyingRepository = study,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            users,
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = onSessionExpired()
            }
        )
    )

    private fun testUser() = User.create().copy(
        uid = "uid-1",
        nickname = "test-user",
        activeSessionToken = "session-1"
    )

    /** 두 저장소의 호출을 일어난 순서대로 기록한다. */
    private class CallClock {
        val calls = mutableListOf<String>()
    }
}
