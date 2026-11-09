package com.a32b.plant.presentation.auth.viewmodel

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.auth.ResolveUserSessionUseCase
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import com.a32b.plant.domain.util.NicknameRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 첫 로그인 닉네임 설정(confirmNickname)의 결과 표시 테스트.
 * 닉네임 값 자체의 문제(형식 오류·중복)는 입력칸 인라인 에러, 그 밖의 실패는 토스트로 보여야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    // 성공 경로는 SetNicknameUseCase가 전역 CurrentUser를 바꾸므로 테스트 전후로 비운다.
    @Before
    fun setUp() {
        CurrentUser.clear()
    }

    @After
    fun tearDown() {
        CurrentUser.clear()
    }

    @Test
    fun `invalid length shows input error without toast or saving`() = runSignInTest { users, viewModel, events ->
        viewModel.onNicknameChange("a")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(NicknameRule.LENGTH_MESSAGE, viewModel.uiState.value.nicknameError)
        assertEquals(emptyList<SignInEvent>(), events)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
        assertTrue(users.completedNicknames.isEmpty())
    }

    @Test
    fun `unusable nickname shows input error without toast or saving`() = runSignInTest { users, viewModel, events ->
        viewModel.onNicknameChange("새/싹")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(NicknameRule.UNUSABLE_MESSAGE, viewModel.uiState.value.nicknameError)
        assertEquals(emptyList<SignInEvent>(), events)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
        assertTrue(users.completedNicknames.isEmpty())
    }

    @Test
    fun `taken nickname shows input error without toast`() = runSignInTest(
        completeResult = Result.Failure(AppError.NicknameTaken())
    ) { _, viewModel, events ->
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(AppError.NicknameTaken().message, viewModel.uiState.value.nicknameError)
        assertEquals(emptyList<SignInEvent>(), events)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
    }

    @Test
    fun `network failure shows long toast without input error`() = runSignInTest(
        completeResult = Result.Failure(AppError.Network())
    ) { _, viewModel, events ->
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()

        assertNull(viewModel.uiState.value.nicknameError)
        assertEquals(listOf(SignInEvent.ShowToast(AppError.Network().message, isLong = true)), events)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
    }

    @Test
    fun `other save failure shows short toast without input error`() = runSignInTest(
        completeResult = Result.Failure(AppError.Custom("닉네임 설정에 실패했습니다."))
    ) { _, viewModel, events ->
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()

        assertNull(viewModel.uiState.value.nicknameError)
        assertEquals(listOf(SignInEvent.ShowToast("닉네임 설정에 실패했습니다.", isLong = false)), events)
    }

    @Test
    fun `success shows welcome toast with saved nickname and navigates home`() = runSignInTest { users, viewModel, events ->
        // 앞뒤 공백 + 자모로 나뉜 "새싹" (U+1109 U+1162 U+110A U+1161 U+11A8)
        viewModel.onNicknameChange(" 새싹 ")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(listOf("새싹"), users.completedNicknames.map { it.second })
        // 토스트는 sendToast가 별도 코루틴으로 보내므로 홈 이동과의 순서는 보장되지 않는다 → 포함 여부만 확인
        assertEquals(
            setOf(SignInEvent.ShowToast("새싹님 환영합니다.", isLong = false), SignInEvent.NavigateToHome),
            events.toSet()
        )
        assertEquals(2, events.size)
        assertFalse(viewModel.uiState.value.showNicknameDialog)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
    }

    @Test
    fun `typing again clears input error`() = runSignInTest { _, viewModel, _ ->
        viewModel.onNicknameChange("a")
        viewModel.confirmNickname()
        runCurrent()
        assertEquals(NicknameRule.LENGTH_MESSAGE, viewModel.uiState.value.nicknameError)

        viewModel.onNicknameChange("ab")

        assertNull(viewModel.uiState.value.nicknameError)
    }

    @Test
    fun `repeated confirm while saving sends only one request`() = runSignInTest { users, viewModel, _ ->
        val gate = CompletableDeferred<Unit>()
        users.completeGate = gate
        viewModel.onNicknameChange("새싹")

        viewModel.confirmNickname()
        runCurrent()
        assertTrue(viewModel.uiState.value.isNicknameLoading)
        viewModel.confirmNickname()
        runCurrent()

        gate.complete(Unit)
        runCurrent()

        assertEquals(1, users.completedNicknames.size)
    }

    private fun runSignInTest(
        completeResult: Result<Unit> = Result.Success(Unit),
        block: suspend TestScope.(FakeUserRepository, SignInViewModel, List<SignInEvent>) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(completeResult)
            val viewModel = createViewModel(users)
            val events = collectEvents(viewModel, dispatcher)
            block(users, viewModel, events)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(users: FakeUserRepository) = SignInViewModel(
        signInWithGoogleUseCase = SignInWithGoogleUseCase(FakeAuthRepository(), ResolveUserSessionUseCase(users)),
        setNicknameUseCase = SetNicknameUseCase(users)
    )

    private fun TestScope.collectEvents(
        viewModel: SignInViewModel,
        dispatcher: TestDispatcher
    ): MutableList<SignInEvent> {
        val events = mutableListOf<SignInEvent>()
        backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }
        return events
    }

    private class FakeUserRepository(
        private val completeResult: Result<Unit>
    ) : UserRepository {
        override val currentUser = MutableStateFlow<User?>(null)
        val completedNicknames = mutableListOf<Pair<String, String>>()
        var completeGate: CompletableDeferred<Unit>? = null

        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User): Unit = error("Unused")
        override fun endUserSession(): Unit = error("Unused")
        override fun takeExpiredSessionReason(): SessionExpiredReason? = error("Unused")
        override suspend fun updateLastSelectedPot(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> {
            completedNicknames += uid to nickname
            completeGate?.await()
            return completeResult
        }
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }

    private class FakeAuthRepository : AuthRepository {
        override fun currentUid(): String? = error("Unused")
        override fun currentEmail(): String? = error("Unused")
        override fun hasSession(): Boolean = error("Unused")
        override fun getSignInProvider(): String? = error("Unused")
        override suspend fun signInWithGoogle(idToken: String): Result<String> = error("Unused")
        override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = error("Unused")
        override suspend fun refreshIdToken(): Result<Unit> = error("Unused")
        override suspend fun deleteAccount(): Result<Unit> = error("Unused")
        override fun signOut(): Unit = error("Unused")
    }
}
