package com.a32b.plant.presentation.mypage.viewmodel

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.usecase.mypage.GetProfileImageLevelListUseCase
import com.a32b.plant.domain.usecase.mypage.ObserveDarkModeUseCase
import com.a32b.plant.domain.usecase.mypage.UpdateProfileUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.NicknameRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class MyPageViewModelTest {
    @Test
    fun `too short nickname shows input error without toast`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser())
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("a", "1")
            runCurrent()

            assertEquals(NicknameRule.LENGTH_MESSAGE, viewModel.uiState.value.nicknameError)
            assertEquals(emptyList<MyPageEvent>(), events)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `taken nickname shows input error without toast`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser(), isNicknameTakenResult = Result.Success(true))
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("new-nick", "1")
            runCurrent()

            assertEquals(AppError.NicknameTaken().message, viewModel.uiState.value.nicknameError)
            assertEquals(emptyList<MyPageEvent>(), events)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `network failure while checking nickname shows long toast without input error`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(
                testUser(),
                isNicknameTakenResult = Result.Failure(AppError.Network())
            )
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("new-nick", "1")
            runCurrent()

            assertNull(viewModel.uiState.value.nicknameError)
            assertEquals(
                listOf(MyPageEvent.ShowToast(AppError.Network().message, true)),
                events
            )
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `image only save failure shows short toast without input error`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(
                testUser(),
                updateProfileResult = Result.Failure(AppError.Custom("프로필 수정에 실패했습니다."))
            )
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("test-user", "2")
            runCurrent()

            assertNull(viewModel.uiState.value.nicknameError)
            assertEquals(
                listOf(MyPageEvent.ShowToast("프로필 수정에 실패했습니다.", false)),
                events
            )
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `unknown user shows no toast and no input error`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(initialUser = null)
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("new-nick", "1")
            runCurrent()

            assertEquals(emptyList<MyPageEvent>(), events)
            assertNull(viewModel.uiState.value.nicknameError)
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(users: FakeUserRepository): MyPageViewModel {
        val notifier = NoopSessionExpiredNotifier
        return MyPageViewModel(
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(users, notifier),
            getProfileImageLevelListUseCase = GetProfileImageLevelListUseCase(FakePotRepository()),
            signOutUseCase = SignOutUseCase(FakeAuthRepository(), users),
            userRepository = users,
            updateProfileUseCase = UpdateProfileUseCase(users, EnsureCurrentUserUseCase(users, notifier)),
            observeDarkModeUseCase = ObserveDarkModeUseCase(users)
        )
    }

    private fun TestScope.collectEvents(
        viewModel: MyPageViewModel,
        dispatcher: TestDispatcher
    ): MutableList<MyPageEvent> {
        val events = mutableListOf<MyPageEvent>()
        backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }
        return events
    }

    private fun testUser() = User.create().copy(
        uid = "uid-1",
        nickname = "test-user",
        profileImg = "1",
        activeSessionToken = "session-1"
    )

    private object NoopSessionExpiredNotifier : SessionExpiredNotifier {
        override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
    }

    private class FakeUserRepository(
        initialUser: User?,
        private val isNicknameTakenResult: Result<Boolean> = Result.Success(false),
        private val updateProfileResult: Result<Unit> = Result.Success(Unit)
    ) : UserRepository {
        override val currentUser = MutableStateFlow(initialUser)

        // 끝나는 Flow면 ObserveDarkModeUseCase가 5초 뒤 재구독을 예약해, 테스트 종료 후 Main이 복원된 뒤 남은 지연이 실행된다.
        override fun observeDarkMode(): Flow<Boolean> = flow { awaitCancellation() }
        override fun startUserSession(user: User): Unit = error("Unused")
        override fun endUserSession(): Unit = error("Unused")
        override fun beginAccountDeletion(uid: String): Unit = error("Unused")
        override fun endAccountDeletion(): Unit = error("Unused")
        override suspend fun updateLastSelectedPot(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = updateProfileResult
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = isNicknameTakenResult
        override suspend fun registerNickname(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun deleteNickname(nickname: String): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
        override suspend fun deleteUserData(uid: String): Result<Unit> = error("Unused")
    }

    private class FakeAuthRepository : AuthRepository {
        override fun currentUid(): String? = error("Unused")
        override fun currentEmail(): String? = error("Unused")
        override fun hasSession(): Boolean = error("Unused")
        override fun getSignInProvider(): String? = error("Unused")
        override suspend fun signInWithGoogle(idToken: String): Result<String> = error("Unused")
        override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = error("Unused")
        override suspend fun deleteAuthAccount(): Result<Unit> = error("Unused")
        override fun signOut(): Unit = error("Unused")
    }

    private class FakePotRepository : PotRepository {
        override fun getPots(uid: String): Flow<List<Pot>> = error("Unused")
        override fun getAvailableTags(): Flow<List<Tag>> = error("Unused")
        override suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> = error("Unused")
        override suspend fun getUserPotById(uid: String, potId: String): Pot? = error("Unused")
        override suspend fun getDuplicationLevelList(uid: String): List<Int> = error("Unused")
        override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> = error("Unused")
        override suspend fun updatePotName(uid: String, potId: String, newName: String): Unit = error("Unused")
        override suspend fun deleteEntirePot(uid: String, potId: String, totalStudyingTime: Long): Unit = error("Unused")
        override suspend fun completeStudyPlan(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> =
            error("Unused")
        override suspend fun levelUpWithItemConsumption(
            uid: String,
            potId: String,
            newLevel: Int,
            items: Map<ItemType, Int>
        ): Result<Unit> = error("Unused")
    }
}
