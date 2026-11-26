package com.a32b.plant.presentation.mypage.viewmodel

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.usecase.mypage.GetProfileImageLevelListUseCase
import com.a32b.plant.domain.usecase.mypage.ObserveDarkModeUseCase
import com.a32b.plant.domain.usecase.mypage.UpdateProfileUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.NicknameRule
import com.a32b.plant.fake.FakeAuthRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import org.junit.Assert.assertTrue
import org.junit.Test

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
            val users = FakeUserRepository(testUser(), updateProfileResult = Result.Failure(AppError.NicknameTaken()))
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
    fun `network failure while saving nickname shows long toast without input error`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(
                testUser(),
                updateProfileResult = Result.Failure(AppError.Network())
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
                updateProfileResult = Result.Failure(AppError.Custom("프로필 저장에 실패했습니다. 잠시 후 다시 시도해주세요."))
            )
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.updateProfile("test-user", "2")
            runCurrent()

            assertNull(viewModel.uiState.value.nicknameError)
            assertEquals(
                listOf(MyPageEvent.ShowToast("프로필 저장에 실패했습니다. 잠시 후 다시 시도해주세요.", false)),
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

    @Test
    fun `reset nickname error clears late input error left after dialog was closed`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val gate = CompletableDeferred<Unit>()
            val users = FakeUserRepository(
                testUser(),
                updateProfileResult = Result.Failure(AppError.NicknameTaken()),
                updateProfileGate = gate
            )
            val viewModel = createViewModel(users)

            viewModel.updateProfile("new-nick", "1")
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)

            viewModel.clearProfileState()

            gate.complete(Unit)
            runCurrent()
            assertEquals(AppError.NicknameTaken().message, viewModel.uiState.value.nicknameError)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.resetNicknameError()
            assertNull(viewModel.uiState.value.nicknameError)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `current user change after screen entry updates nickname and profile image`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser())
            val viewModel = createViewModel(users)
            assertEquals("test-user", viewModel.uiState.value.nickname)

            // 시간 초과 뒤 늦게 반영된 저장을 실시간 구독이 currentUser에 전달한 상황
            users.currentUser.value = testUser().copy(nickname = "late-nick", profileImg = "3")
            runCurrent()

            assertEquals("late-nick", viewModel.uiState.value.nickname)
            assertEquals("3", viewModel.uiState.value.profileImg)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `null current user keeps existing nickname and profile image`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser())
            val viewModel = createViewModel(users)

            // 로그아웃·세션 만료 때는 currentUser가 null이 된다
            users.currentUser.value = null
            runCurrent()

            assertEquals("test-user", viewModel.uiState.value.nickname)
            assertEquals("1", viewModel.uiState.value.profileImg)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `loads profile levels through the saved unlocked level`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser().copy(unlockedProfileLevel = 2))
            val viewModel = createViewModel(users)

            viewModel.getImageLevelList()

            assertEquals(listOf(0, 1, 2), viewModel.uiState.value.levelList)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `falls back to level zero when the current user disappears`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(testUser().copy(unlockedProfileLevel = 2))
            val viewModel = createViewModel(users)

            viewModel.getImageLevelList()
            assertEquals(listOf(0, 1, 2), viewModel.uiState.value.levelList)

            users.currentUser.value = null
            viewModel.getImageLevelList()

            assertEquals(listOf(0), viewModel.uiState.value.levelList)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // 주소는 hosting/build-legal.mjs의 PAGES 출력 파일 이름과 같아야 한다 (#607)
    @Test
    fun `privacy policy and terms buttons open their web pages`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val viewModel = createViewModel(FakeUserRepository(testUser()))
            val events = collectEvents(viewModel, dispatcher)

            viewModel.onPrivacyPolicyClick()
            viewModel.onTermsOfServiceClick()
            runCurrent()

            assertEquals(
                listOf(
                    MyPageEvent.OpenUrl("https://plant-82b09.web.app/privacy-policy.html"),
                    MyPageEvent.OpenUrl("https://plant-82b09.web.app/terms.html")
                ),
                events
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    // 로그아웃 확인 연타 (#672): 토큰 쓰기를 기다리는 동안 다시 눌러도 한 번만 실행되고 이동 이벤트도 한 번만 간다
    @Test
    fun `logout pressed twice while clearing token runs once`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val tokenGate = CompletableDeferred<Unit>()
            val users = FakeUserRepository(testUser(), updateSessionTokenGate = tokenGate)
            val auth = FakeAuthRepository()
            val viewModel = createViewModel(users, auth)
            val events = collectEvents(viewModel, dispatcher)

            viewModel.logout()
            viewModel.logout()   // 토큰 쓰기 대기 중 (게이트 미완료)
            runCurrent()
            tokenGate.complete(Unit)
            runCurrent()
            viewModel.logout()   // 끝난 뒤(화면 이동 전) 한 번 더
            runCurrent()

            assertEquals(listOf("uid-1" to null), users.sessionTokenUpdates)
            assertEquals(1, auth.signOutCalls)
            assertEquals(listOf<MyPageEvent>(MyPageEvent.NavigateToSignIn), events)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // 로그아웃 대기 중 화면 이탈 방지 (#791): 화면은 isLoggingOut 동안 확인 다이얼로그를 로딩 상태로 유지한다.
    // 토큰 쓰기를 기다리는 동안 켜져 있어야 하고, 끝난 뒤에도 로그인 화면으로 이동할 때까지 꺼지지 않아야 한다
    @Test
    fun `isLoggingOut stays true from confirm until navigating to sign in`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val tokenGate = CompletableDeferred<Unit>()
            val users = FakeUserRepository(testUser(), updateSessionTokenGate = tokenGate)
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)

            assertEquals(false, viewModel.uiState.value.isLoggingOut)

            viewModel.logout()
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isLoggingOut)   // 토큰 쓰기 대기 중
            assertEquals(emptyList<MyPageEvent>(), events)

            tokenGate.complete(Unit)
            runCurrent()
            assertEquals(true, viewModel.uiState.value.isLoggingOut)   // 끝난 뒤(화면 이동 전)
            assertEquals(listOf<MyPageEvent>(MyPageEvent.NavigateToSignIn), events)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(
        users: FakeUserRepository,
        auth: FakeAuthRepository = FakeAuthRepository()
    ): MyPageViewModel {
        val notifier = NoopSessionExpiredNotifier
        // 끝나는 Flow면 ObserveDarkModeUseCase가 5초 뒤 재구독을 예약해, 테스트 종료 후 Main이 복원된 뒤 남은 지연이 실행된다.
        users.darkModeFlow = flow { awaitCancellation() }
        return MyPageViewModel(
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(users, notifier),
            getProfileImageLevelListUseCase = GetProfileImageLevelListUseCase(EnsureCurrentUserUseCase(users, notifier)),
            signOutUseCase = SignOutUseCase(auth, users),
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

}
