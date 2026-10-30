package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.StudyLog
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
    fun `failed cleanup keeps current user and a later invocation succeeds`() = runBlocking {
        val user = testUser(token = "session-1")
        val users = FakeUserRepository(user)
        val useCase = createUseCase(users, FakeAuthRepository("uid-1"))

        assertTrue(useCase() is Result.Failure)
        assertTrue(users.startedSessions.isEmpty())
        assertEquals(0, users.endUserSessionCalls)
        assertEquals(user, users.currentUser.value)
        assertEquals(listOf("begin", "deleteUserData", "end"), users.callOrder)

        users.deleteUserDataResult = Result.Success(Unit)
        users.deleteNicknameResult = Result.Success(Unit)

        assertTrue(useCase() is Result.Success)
        assertTrue(users.startedSessions.isEmpty())
        assertEquals(1, users.endUserSessionCalls)
        assertTrue(users.currentUser.value == null)
        assertEquals(
            listOf("begin", "deleteUserData", "end", "begin", "deleteUserData", "endUserSession", "deleteNickname", "end"),
            users.callOrder
        )
    }

    @Test
    fun `nickname deletion failure does not restore deleted user session`() = runBlocking {
        val users = FakeUserRepository(
            testUser("session-1"),
            deleteUserDataResult = Result.Success(Unit),
            deleteNicknameResult = Result.Failure(AppError.Network())
        )
        val auth = FakeAuthRepository("uid-1")
        val useCase = createUseCase(users, auth)

        val result = useCase()

        assertTrue(result is Result.Failure)
        assertTrue(users.startedSessions.isEmpty())
        assertTrue(users.currentUser.value == null)
        assertEquals(0, auth.deleteAuthAccountCalls)
        assertEquals(
            listOf("begin", "deleteUserData", "endUserSession", "deleteNickname", "end"),
            users.callOrder
        )
    }

    @Test
    fun `auth deletion failure does not restore deleted user document`() = runBlocking {
        val users = FakeUserRepository(
            testUser("session-1"),
            deleteUserDataResult = Result.Success(Unit),
            deleteNicknameResult = Result.Success(Unit)
        )
        val study = FakeStudyingRepository()
        val useCase = createUseCase(users, FakeAuthRepository("uid-1", Result.Failure(AppError.Auth())), study)

        val result = useCase()

        assertTrue(result is Result.Failure)
        assertTrue(users.startedSessions.isEmpty())
        assertEquals(1, users.endUserSessionCalls)
        assertTrue(users.currentUser.value == null)
        assertTrue(users.darkModeUpdates.isEmpty())
        assertEquals(0, study.clearLocalSessionCalls)
    }

    @Test
    fun `cancellation before document deletion preserves user and retry succeeds`() = runBlocking {
        val user = testUser("session-1")
        var cancelFirstCall = true
        val users = FakeUserRepository(
            user,
            deleteUserDataResult = Result.Success(Unit),
            deleteNicknameResult = Result.Success(Unit),
            onDeleteUserData = {
                if (cancelFirstCall) {
                    cancelFirstCall = false
                    throw CancellationException("test cancellation")
                }
            }
        )
        val useCase = createUseCase(users, FakeAuthRepository("uid-1", Result.Success(Unit)))

        val cancelled = try {
            useCase()
            false
        } catch (_: CancellationException) {
            true
        }

        assertTrue(cancelled)
        assertEquals(1, users.endDeletionCalls)
        assertTrue(users.startedSessions.isEmpty())
        assertEquals(0, users.endUserSessionCalls)
        assertEquals(user, users.currentUser.value)

        assertTrue(useCase() is Result.Success)
        assertEquals(2, users.endDeletionCalls)
        assertTrue(users.startedSessions.isEmpty())
        assertEquals(1, users.endUserSessionCalls)
        assertTrue(users.currentUser.value == null)
    }

    @Test
    fun `successful deletion ends session and clears local settings`() = runBlocking {
        val users = FakeUserRepository(
            testUser("session-1"),
            deleteUserDataResult = Result.Success(Unit),
            deleteNicknameResult = Result.Success(Unit)
        )
        val study = FakeStudyingRepository()
        val useCase = createUseCase(users, FakeAuthRepository("uid-1", Result.Success(Unit)), study)

        assertTrue(useCase() is Result.Success)

        assertEquals(1, users.endUserSessionCalls)
        assertEquals(listOf(false), users.darkModeUpdates)
        assertEquals(1, study.clearLocalSessionCalls)
        assertTrue(users.currentUser.value == null)
    }

    private fun createUseCase(
        users: FakeUserRepository,
        auth: AuthRepository,
        study: StudyingRepository = FakeStudyingRepository()
    ) = DeleteAccountUseCase(
        authRepository = auth,
        userRepository = users,
        studyingRepository = study,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            users,
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
    )

    private fun testUser(token: String) = User.create().copy(
        uid = "uid-1",
        nickname = "test-user",
        activeSessionToken = token
    )

    private class FakeUserRepository(
        initialUser: User?,
        var deleteUserDataResult: Result<Unit> = Result.Failure(AppError.Network()),
        var deleteNicknameResult: Result<Unit> = Result.Failure(AppError.Network()),
        private val onDeleteUserData: suspend FakeUserRepository.() -> Unit = {}
    ) : UserRepository {
        override val currentUser = MutableStateFlow(initialUser)
        val callOrder = mutableListOf<String>()
        val startedSessions = mutableListOf<User>()
        val darkModeUpdates = mutableListOf<Boolean>()
        var endDeletionCalls = 0
        var endUserSessionCalls = 0

        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User) {
            callOrder += "startUserSession"
            startedSessions += user
            currentUser.value = user
        }
        override fun endUserSession() {
            callOrder += "endUserSession"
            endUserSessionCalls++
            currentUser.value = null
        }
        override fun beginAccountDeletion(uid: String) { callOrder += "begin" }
        override fun endAccountDeletion() {
            callOrder += "end"
            endDeletionCalls++
        }
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
        override suspend fun registerNickname(nickname: String): Result<Unit> = TODO()
        override suspend fun deleteNickname(nickname: String): Result<Unit> {
            callOrder += "deleteNickname"
            return deleteNicknameResult
        }
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = TODO()
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = TODO()
        override suspend fun deleteUserData(uid: String): Result<Unit> {
            callOrder += "deleteUserData"
            onDeleteUserData()
            return deleteUserDataResult
        }
    }

    private class FakeAuthRepository(
        private val uid: String?,
        private val deleteAuthResult: Result<Unit> = Result.Success(Unit)
    ) : AuthRepository {
        var deleteAuthAccountCalls = 0

        override fun currentUid(): String? = uid
        override fun currentEmail(): String? = null
        override fun hasSession(): Boolean = uid != null
        override fun getSignInProvider(): String? = "google.com"
        override suspend fun signInWithGoogle(idToken: String): Result<String> = TODO()
        override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = TODO()
        override suspend fun deleteAuthAccount(): Result<Unit> {
            deleteAuthAccountCalls++
            return deleteAuthResult
        }
        override fun signOut() = Unit
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
    }
}
