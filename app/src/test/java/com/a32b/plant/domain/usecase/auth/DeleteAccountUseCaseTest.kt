package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingGoalDraft
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteAccountUseCaseTest {
    @Test
    fun `successful deletion ends session signs out and clears local settings`() = runBlocking {
        val clock = CallClock()
        val users = FakeUserRepository(testUser(), clock)
        val auth = FakeAuthRepository(clock)
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
        val users = FakeUserRepository(user, clock)
        val auth = FakeAuthRepository(clock, deleteAccountResult = Result.Failure(AppError.Network()))
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
        val users = FakeUserRepository(testUser(), clock)
        val auth = FakeAuthRepository(clock, deleteAccountResult = Result.Failure(AppError.Custom(message)))
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
        val users = FakeUserRepository(user, clock)
        val auth = FakeAuthRepository(clock, refreshResult = Result.Failure(AppError.Network()))
        val useCase = createUseCase(users, auth)

        assertEquals(Result.Failure(AppError.Network()), useCase())

        assertEquals(listOf("refreshIdToken"), clock.calls)
        assertEquals(user, users.currentUser.value)
    }

    @Test
    fun `already deleted auth account is treated as completed deletion`() = runBlocking {
        val clock = CallClock()
        val users = FakeUserRepository(testUser(), clock)
        val auth = FakeAuthRepository(clock, refreshResult = Result.Failure(AppError.AccountNotFound()))
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
        val users = FakeUserRepository(user, clock)
        val auth = FakeAuthRepository(clock, onDeleteAccount = { throw CancellationException("test cancellation") })
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
        val users = FakeUserRepository(null, clock)
        val auth = FakeAuthRepository(clock)
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

    private class FakeUserRepository(
        initialUser: User?,
        private val clock: CallClock
    ) : UserRepository {
        override val currentUser = MutableStateFlow(initialUser)
        val startedSessions = mutableListOf<User>()
        val darkModeUpdates = mutableListOf<Boolean>()

        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User) {
            clock.calls += "startUserSession"
            startedSessions += user
            currentUser.value = user
        }
        override fun endUserSession() {
            clock.calls += "endUserSession"
            currentUser.value = null
        }
        override fun takeExpiredSessionReason(): SessionExpiredReason? = TODO()
        override suspend fun updateLastSelectedPot(uid: String, potId: String) = Unit
        override suspend fun getUser(uid: String): Result<User?> = TODO()
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = TODO()
        override suspend fun createUser(uid: String): Result<User> = TODO()
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = TODO()
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> {
            darkModeUpdates += isDarkMode
            return Result.Success(Unit)
        }
        override suspend fun updateProfile(user: User): Result<Unit> = TODO()
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = TODO()
        override suspend fun registerNickname(uid: String, nickname: String): Result<Unit> = TODO()
        override suspend fun deleteNickname(nickname: String): Result<Unit> = TODO()
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = TODO()
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = TODO()
    }

    private class FakeAuthRepository(
        private val clock: CallClock,
        private val refreshResult: Result<Unit> = Result.Success(Unit),
        var deleteAccountResult: Result<Unit> = Result.Success(Unit),
        private val onDeleteAccount: suspend () -> Unit = {}
    ) : AuthRepository {
        var deleteAccountCalls = 0
        var signOutCalls = 0

        override fun currentUid(): String? = "uid-1"
        override fun currentEmail(): String? = null
        override fun hasSession(): Boolean = true
        override fun getSignInProvider(): String? = "google.com"
        override suspend fun signInWithGoogle(idToken: String): Result<String> = TODO()
        override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = TODO()
        override suspend fun refreshIdToken(): Result<Unit> {
            clock.calls += "refreshIdToken"
            return refreshResult
        }
        override suspend fun deleteAccount(): Result<Unit> {
            clock.calls += "deleteAccount"
            deleteAccountCalls++
            onDeleteAccount()
            return deleteAccountResult
        }
        override fun signOut() {
            clock.calls += "signOut"
            signOutCalls++
        }
    }

    private class FakeStudyingRepository : StudyingRepository {
        var clearLocalSessionCalls = 0
        override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> = TODO()
        override suspend fun initStudyingUser(user: StudyingUser): Result<Unit> = TODO()
        override suspend fun updateStudyingTime(tag: String, time: Long): Result<Unit> = TODO()
        override suspend fun updateTotalStudyTime(potId: String, studyTime: Long): Result<Unit> = TODO()
        override suspend fun updateUserTotalStudyTime(time: Long): Result<Unit> = TODO()
        override suspend fun saveStudyLog(potId: String, log: StudyLog): Result<Unit> = TODO()
        override suspend fun deleteStudyingUserInfo(): Result<Unit> = TODO()
        override suspend fun saveLocalSession(studying: StudyingSession): Result<Unit> = TODO()
        override suspend fun readLocalSession(): Result<StudyingSession?> = TODO()
        override suspend fun clearLocalSession(): Result<Unit> {
            clearLocalSessionCalls++
            return Result.Success(Unit)
        }
        override suspend fun saveLocalGoalDraft(draft: StudyingGoalDraft): Result<Unit> = TODO()
        override suspend fun readLocalGoalDraft(): Result<StudyingGoalDraft?> = TODO()
        override suspend fun clearLocalGoalDraft(): Result<Unit> = TODO()
    }
}
