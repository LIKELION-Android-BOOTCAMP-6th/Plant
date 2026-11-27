package com.a32b.plant.presentation.auth.viewmodel

import com.a32b.plant.core.util.LegalUrls
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.auth.ResolveUserSessionUseCase
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.util.NicknameRule
import com.a32b.plant.fake.FakeAuthRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * 필수 동의(만 14세 이상·이용약관·개인정보처리방침) 체크와 다이얼로그 로그아웃도 함께 확인한다 (#661).
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
        completeResult = Result.Failure(AppError.Custom("닉네임 설정에 실패했어요."))
    ) { _, viewModel, events ->
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()

        assertNull(viewModel.uiState.value.nicknameError)
        assertEquals(listOf(SignInEvent.ShowToast("닉네임 설정에 실패했어요.", isLong = false)), events)
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
            setOf(SignInEvent.ShowToast("새싹님 환영해요.", isLong = false), SignInEvent.NavigateToHome),
            events.toSet()
        )
        assertEquals(2, events.size)
        assertFalse(viewModel.uiState.value.showNicknameDialog)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
    }

    @Test
    fun `already completed shows welcome toast with the saved nickname (#661)`() = runSignInTest(
        completeResult = Result.Success("이전닉")
    ) { _, viewModel, events ->
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(
            setOf(SignInEvent.ShowToast("이전닉님 환영해요.", isLong = false), SignInEvent.NavigateToHome),
            events.toSet()
        )
        assertEquals(2, events.size)
        assertFalse(viewModel.uiState.value.showNicknameDialog)
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
        users.completeFirstLoginGate = gate
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

    // ----- 필수 동의 (#661) -----

    @Test
    fun `confirm without all required consents shows toast and does not save`() = runSignInTest(
        agreeAll = false
    ) { users, viewModel, events ->
        viewModel.onNicknameChange("새싹")

        // 하나도 체크하지 않음
        viewModel.confirmNickname()
        runCurrent()
        // 세 항목 중 두 개만 체크 (만 14세 미만이라 나이 항목을 체크하지 않은 경우)
        viewModel.onConsentChange(SignUpConsent.TERMS_OF_SERVICE, true)
        viewModel.onConsentChange(SignUpConsent.PRIVACY_POLICY, true)
        viewModel.confirmNickname()
        runCurrent()

        val toast = SignInEvent.ShowToast(AppError.RequiredConsentMissing().message, isLong = false)
        assertEquals(listOf(toast, toast), events)
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(viewModel.uiState.value.nicknameError)
        assertFalse(viewModel.uiState.value.isNicknameLoading)
        // 입력한 닉네임과 체크한 항목은 그대로 남는다
        assertEquals("새싹", viewModel.uiState.value.nicknameInput)
        assertEquals(
            setOf(SignUpConsent.TERMS_OF_SERVICE, SignUpConsent.PRIVACY_POLICY),
            viewModel.uiState.value.agreedConsents
        )
    }

    @Test
    fun `all consent checks and unchecks every item`() = runSignInTest(agreeAll = false) { _, viewModel, _ ->
        viewModel.onConsentChange(SignUpConsent.AGE_14_OR_OVER, true)

        viewModel.onAllConsentChange(true)
        assertEquals(SignUpConsent.entries.toSet(), viewModel.uiState.value.agreedConsents)
        assertTrue(viewModel.uiState.value.isAllConsented)

        viewModel.onAllConsentChange(false)
        assertEquals(emptySet<SignUpConsent>(), viewModel.uiState.value.agreedConsents)
        assertFalse(viewModel.uiState.value.isAllConsented)
    }

    @Test
    fun `checking every item one by one turns all consent on, unchecking one turns it off`() = runSignInTest(
        agreeAll = false
    ) { _, viewModel, _ ->
        viewModel.onConsentChange(SignUpConsent.AGE_14_OR_OVER, true)
        viewModel.onConsentChange(SignUpConsent.TERMS_OF_SERVICE, true)
        assertFalse(viewModel.uiState.value.isAllConsented)

        viewModel.onConsentChange(SignUpConsent.PRIVACY_POLICY, true)
        assertTrue(viewModel.uiState.value.isAllConsented)

        viewModel.onConsentChange(SignUpConsent.TERMS_OF_SERVICE, false)
        assertFalse(viewModel.uiState.value.isAllConsented)
    }

    @Test
    fun `nickname input error keeps consents`() = runSignInTest { _, viewModel, _ ->
        viewModel.onNicknameChange("a")
        viewModel.confirmNickname()
        runCurrent()

        assertEquals(NicknameRule.LENGTH_MESSAGE, viewModel.uiState.value.nicknameError)
        assertTrue(viewModel.uiState.value.isAllConsented)
    }

    @Test
    fun `consent changes are ignored while saving`() = runSignInTest { users, viewModel, _ ->
        val gate = CompletableDeferred<Unit>()
        users.completeFirstLoginGate = gate
        viewModel.onNicknameChange("새싹")
        viewModel.confirmNickname()
        runCurrent()
        assertTrue(viewModel.uiState.value.isNicknameLoading)

        viewModel.onAllConsentChange(false)
        viewModel.onConsentChange(SignUpConsent.AGE_14_OR_OVER, false)

        assertTrue(viewModel.uiState.value.isAllConsented)
        gate.complete(Unit)
        runCurrent()
    }

    @Test
    fun `view buttons open terms and privacy policy pages`() = runSignInTest { _, viewModel, events ->
        viewModel.onTermsOfServiceClick()
        viewModel.onPrivacyPolicyClick()
        runCurrent()

        assertEquals(
            listOf(SignInEvent.OpenUrl(LegalUrls.TERMS_OF_SERVICE), SignInEvent.OpenUrl(LegalUrls.PRIVACY_POLICY)),
            events
        )
    }

    @Test
    fun `sign out from nickname dialog signs out and clears dialog state`() {
        val auth = FakeAuthRepository()
        runSignInTest(auth = auth) { users, viewModel, events ->
            viewModel.onNicknameChange("새싹")
            viewModel.signOutFromNicknameDialog()
            runCurrent()

            assertEquals(1, auth.signOutCalls)
            assertEquals(SignInUiState(), viewModel.uiState.value)
            assertTrue(users.completedNicknames.isEmpty())
            assertEquals(emptyList<SignInEvent>(), events)
        }
    }

    @Test
    fun `sign out is ignored while saving`() {
        val auth = FakeAuthRepository()
        runSignInTest(auth = auth) { users, viewModel, _ ->
            val gate = CompletableDeferred<Unit>()
            users.completeFirstLoginGate = gate
            viewModel.onNicknameChange("새싹")
            viewModel.confirmNickname()
            runCurrent()

            viewModel.signOutFromNicknameDialog()
            runCurrent()

            assertEquals(0, auth.signOutCalls)
            gate.complete(Unit)
            runCurrent()
        }
    }

    // 로그인한 사용자가 있어야 SignOutUseCase가 세션 토큰 지우기(updateSessionToken)를 기다린다.
    // 게이트로 토큰 지우기를 멈춰 "로그아웃 진행 중" 상태를 만든다

    @Test
    fun `repeated sign out while clearing session token signs out only once`() {
        val auth = FakeAuthRepository()
        runSignInTest(auth = auth) { users, viewModel, _ ->
            val gate = CompletableDeferred<Unit>()
            users.updateSessionTokenGate = gate
            users.currentUser.value = User.create().copy(uid = "uid-1")
            viewModel.onNicknameChange("새싹")

            viewModel.signOutFromNicknameDialog()
            runCurrent()
            viewModel.signOutFromNicknameDialog()
            runCurrent()

            // 토큰 지우기를 기다리는 중: 아직 로그아웃 전이고 다이얼로그 상태도 그대로
            assertEquals(listOf("uid-1" to null), users.sessionTokenUpdates)
            assertEquals(0, auth.signOutCalls)
            assertEquals("새싹", viewModel.uiState.value.nicknameInput)

            gate.complete(Unit)
            runCurrent()

            assertEquals(1, users.sessionTokenUpdates.size)
            assertEquals(1, auth.signOutCalls)
            assertEquals(SignInUiState(), viewModel.uiState.value)
        }
    }

    @Test
    fun `confirm is ignored while signing out`() {
        val auth = FakeAuthRepository()
        runSignInTest(auth = auth) { users, viewModel, events ->
            val gate = CompletableDeferred<Unit>()
            users.updateSessionTokenGate = gate
            users.currentUser.value = User.create().copy(uid = "uid-1")
            viewModel.onNicknameChange("새싹")

            viewModel.signOutFromNicknameDialog()
            runCurrent()
            viewModel.confirmNickname()
            runCurrent()

            assertTrue(users.completedNicknames.isEmpty())
            assertFalse(viewModel.uiState.value.isNicknameLoading)

            gate.complete(Unit)
            runCurrent()

            assertTrue(users.completedNicknames.isEmpty())
            assertEquals(1, auth.signOutCalls)
            assertEquals(emptyList<SignInEvent>(), events)
        }
    }

    /**
     * [agreeAll]이 true면 필수 동의 세 항목을 모두 체크한 상태로 시작한다 (동의와 무관한 기존 닉네임 테스트용)
     */
    private fun runSignInTest(
        completeResult: Result<String>? = null,
        agreeAll: Boolean = true,
        auth: FakeAuthRepository = FakeAuthRepository(),
        block: suspend TestScope.(FakeUserRepository, SignInViewModel, List<SignInEvent>) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val users = FakeUserRepository(completeFirstLoginResult = completeResult)
            val viewModel = createViewModel(users, auth)
            val events = collectEvents(viewModel, dispatcher)
            if (agreeAll) viewModel.onAllConsentChange(true)
            block(users, viewModel, events)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(users: FakeUserRepository, auth: FakeAuthRepository) = SignInViewModel(
        signInWithGoogleUseCase = SignInWithGoogleUseCase(auth, ResolveUserSessionUseCase(users)),
        setNicknameUseCase = SetNicknameUseCase(users),
        signOutUseCase = SignOutUseCase(auth, users)
    )

    private fun TestScope.collectEvents(
        viewModel: SignInViewModel,
        dispatcher: TestDispatcher
    ): MutableList<SignInEvent> {
        val events = mutableListOf<SignInEvent>()
        backgroundScope.launch(dispatcher) { viewModel.events.toList(events) }
        return events
    }
}
