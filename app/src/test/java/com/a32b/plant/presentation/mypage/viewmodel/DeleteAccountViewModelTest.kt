package com.a32b.plant.presentation.mypage.viewmodel

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
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {
    @Test
    fun unknownUserDuringReauthenticationNotifiesSessionExpiredWithoutToast() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val users = FakeUserRepository(User.create().copy(uid = "uid-1"))
            val auth = FakeAuthRepository(Result.Failure(AppError.UnknownUser()))
            val useCaseNotifier = CountingSessionExpiredNotifier()
            val viewModelNotifier = CountingSessionExpiredNotifier()
            val viewModel = DeleteAccountViewModel(
                deleteAccountUseCase = DeleteAccountUseCase(
                    authRepository = auth,
                    userRepository = users,
                    studyingRepository = FakeStudyingRepository(),
                    ensureCurrentUserUseCase = EnsureCurrentUserUseCase(users, useCaseNotifier)
                ),
                authRepository = auth,
                sessionExpiredNotifier = viewModelNotifier
            )
            val events = mutableListOf<DeleteAccountEvent>()
            backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }

            viewModel.reauthenticateWithGoogleAndDelete("token")
            runCurrent()

            assertEquals(1, viewModelNotifier.calls)
            assertEquals(0, useCaseNotifier.calls)
            assertEquals(emptyList<DeleteAccountEvent>(), events)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class CountingSessionExpiredNotifier : SessionExpiredNotifier {
        var calls = 0
        override fun notifySessionExpired(reason: SessionExpiredReason) {
            calls++
        }
    }

    private class FakeAuthRepository(
        private val reauthenticationResult: Result<Unit>
    ) : AuthRepository {
        override fun currentUid(): String? = "uid-1"
        override fun currentEmail(): String? = null
        override fun hasSession(): Boolean = true
        override fun getSignInProvider(): String? = "google.com"
        override suspend fun signInWithGoogle(idToken: String): Result<String> = error("Not used")
        override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = reauthenticationResult
        override suspend fun deleteAuthAccount(): Result<Unit> = error("Not used")
        override fun signOut() = Unit
    }

    private class FakeUserRepository(
        user: User?
    ) : UserRepository {
        override val currentUser = MutableStateFlow(user)
        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User) = error("Not used")
        override fun endUserSession() = error("Not used")
        override fun beginAccountDeletion(uid: String) = error("Not used")
        override fun endAccountDeletion() = error("Not used")
        override suspend fun updateLastSelectedPot(uid: String, potId: String) = error("Not used")
        override suspend fun getUser(uid: String): Result<User?> = error("Not used")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Not used")
        override suspend fun createUser(uid: String): Result<User> = error("Not used")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Not used")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Not used")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Not used")
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = error("Not used")
        override suspend fun registerNickname(nickname: String): Result<Unit> = error("Not used")
        override suspend fun deleteNickname(nickname: String): Result<Unit> = error("Not used")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Not used")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Not used")
        override suspend fun deleteUserData(uid: String): Result<Unit> = error("Not used")
    }

    private class FakeStudyingRepository : StudyingRepository {
        override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> = error("Not used")
        override suspend fun initStudyingUser(user: StudyingUser): Result<Unit> = error("Not used")
        override suspend fun updateStudyingTime(tag: String, time: Long): Result<Unit> = error("Not used")
        override suspend fun updateTotalStudyTime(potId: String, studyTime: Long): Result<Unit> = error("Not used")
        override suspend fun updateUserTotalStudyTime(time: Long): Result<Unit> = error("Not used")
        override suspend fun saveStudyLog(potId: String, log: StudyLog): Result<Unit> = error("Not used")
        override suspend fun deleteStudyingUserInfo(): Result<Unit> = error("Not used")
        override suspend fun saveLocalSession(studying: StudyingSession): Result<Unit> = error("Not used")
        override suspend fun readLocalSession(): Result<StudyingSession?> = error("Not used")
        override suspend fun clearLocalSession(): Result<Unit> = error("Not used")
    }
}
