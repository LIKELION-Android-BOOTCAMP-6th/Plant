package com.a32b.plant.presentation.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.LegalUrls
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.util.NicknameRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val isLoading: Boolean = false,
    val showNicknameDialog: Boolean = false,
    val nicknameInput: String = "",
    val nicknameError: String? = null,
    val isNicknameLoading: Boolean = false,
    // 첫 로그인 닉네임 설정 다이얼로그에서 체크한 필수 동의 항목 (#661). 서버에는 저장하지 않고, 모두 동의해야 저장된다
    val agreedConsents: Set<SignUpConsent> = emptySet()
) {
    // "전체 동의" 체크 상태: 세 항목을 하나씩 모두 체크해도 체크 상태가 된다
    val isAllConsented: Boolean
        get() = agreedConsents.containsAll(SignUpConsent.entries)
}

/** 첫 로그인 때 받는 필수 동의 항목 (#661) */
enum class SignUpConsent {
    AGE_14_OR_OVER,
    TERMS_OF_SERVICE,
    PRIVACY_POLICY
}

sealed class SignInEvent {
    data class ShowToast(val message: String, val isLong: Boolean) : SignInEvent()
    data class OpenUrl(val url: String) : SignInEvent()
    object NavigateToHome : SignInEvent()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val setNicknameUseCase: SetNicknameUseCase,
    private val signOutUseCase: SignOutUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private var loggedInUid: String = ""
    private var isSigningOut = false

    init {
        loaded() // 초기에 불러올 데이터가 없으므로 LoadableScreen 로딩 없이 즉시 로그인 UI 표시
    }

    fun onNicknameChange(value: String) {
        _uiState.update {
            it.copy(
                nicknameInput = NicknameRule.limitInput(previous = it.nicknameInput, input = value),
                nicknameError = null
            )
        }
    }

    fun tryStartGoogleSignIn(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update { it.copy(isLoading = true) }
        return true
    }

    fun cancelGoogleSignIn() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun handleGoogleSignIn(idToken: String) {
        if (!_uiState.value.isLoading) return // 현재 호출부에서는 항상 true라 실질적으로 안 타지만, tryStartGoogleSignIn() 없이 직접 호출하는 경로가 생기면 signInWithGoogleUseCase 중복 호출을 막아주는 가드라 지우지 말 것

        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { result -> handleLoginSuccess(result.uid, result.nickname, result.isFirstLogin) }
                .onFailure { error -> sendToast("구글 로그인 실패: ${error.message}") }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleLoginSuccess(uid: String, nickname: String, isFirstLogin: Boolean) {
        loggedInUid = uid

        if (isFirstLogin) {
            // 다이얼로그를 새로 띄울 때마다 동의는 처음부터 받는다
            _uiState.update { it.copy(showNicknameDialog = true, agreedConsents = emptySet()) }
        } else {
            sendToast("${nickname}님 환영해요.")
            viewModelScope.launch { _eventChannel.send(SignInEvent.NavigateToHome) }
        }
    }

    fun confirmNickname() {
        if (_uiState.value.isNicknameLoading || isSigningOut) return

        val nickname = _uiState.value.nicknameInput

        // 형식 오류도 UseCase 결과로 받으므로 가드를 통과한 확인 요청은 로딩 상태를 거친다.
        // 형식 오류는 UseCase가 중간에 멈추지 않고 바로 실패를 돌려줘, 이 클릭 처리 안에서 false로 돌아오므로
        // (viewModelScope는 Dispatchers.Main.immediate) 로딩 표시는 그려지지 않는다. 중복·네트워크 실패는 서버를 기다리는 동안 표시된다.
        _uiState.update { it.copy(isNicknameLoading = true, nicknameError = null) }

        val hasAgreedToRequiredTerms = _uiState.value.isAllConsented

        viewModelScope.launch {
            // 필수 항목에 모두 동의하지 않았으면 UseCase가 저장 없이 AppError.RequiredConsentMissing을 돌려줘 토스트로 안내된다 (#661)
            setNicknameUseCase(loggedInUid, nickname, hasAgreedToRequiredTerms)
                .onSuccess { savedNickname ->
                    _uiState.update { it.copy(showNicknameDialog = false, isNicknameLoading = false) }
                    sendToast("${savedNickname}님 환영해요.")
                    _eventChannel.send(SignInEvent.NavigateToHome)
                }
                .onFailure { error ->
                    // 닉네임 값 자체의 문제(형식 오류·중복)만 입력칸 인라인 에러, 네트워크·저장 실패 등 상황 에러는 토스트
                    val isInputError = error is AppError.NicknameInvalid || error is AppError.NicknameTaken
                    _uiState.update {
                        it.copy(
                            nicknameError = if (isInputError) error.message else null,
                            isNicknameLoading = false
                        )
                    }
                    if (!isInputError) sendToast(error.message, isLong = error is AppError.Network)
                }
        }
    }

    // ----- 필수 동의 (#661) -----
    // 저장 중에는 체크를 바꾸지 않는다 (요청에 실린 동의 상태와 화면이 달라지지 않게)

    fun onConsentChange(consent: SignUpConsent, checked: Boolean) {
        if (_uiState.value.isNicknameLoading) return
        _uiState.update {
            it.copy(agreedConsents = if (checked) it.agreedConsents + consent else it.agreedConsents - consent)
        }
    }

    fun onAllConsentChange(checked: Boolean) {
        if (_uiState.value.isNicknameLoading) return
        _uiState.update {
            it.copy(agreedConsents = if (checked) SignUpConsent.entries.toSet() else emptySet())
        }
    }

    fun onTermsOfServiceClick() {
        viewModelScope.launch { _eventChannel.send(SignInEvent.OpenUrl(LegalUrls.TERMS_OF_SERVICE)) }
    }

    fun onPrivacyPolicyClick() {
        viewModelScope.launch { _eventChannel.send(SignInEvent.OpenUrl(LegalUrls.PRIVACY_POLICY)) }
    }

    /**
     * 닉네임 설정 다이얼로그에서 로그아웃 (만 14세 미만 등 동의하지 않고 나가는 경우)
     * users 문서는 isFirstLogin = true로 남아, 다음 로그인 때 다시 닉네임 설정·동의를 받는다
     * (앞서 시간 초과로 실패 안내된 저장이 서버에 늦게 반영된 경우는 제외: 그때는 첫 로그인이 완료돼 다음 로그인 때 바로 홈으로 간다)
     */
    fun signOutFromNicknameDialog() {
        if (_uiState.value.isNicknameLoading || isSigningOut) return
        // 세션 토큰 지우기(대기 제한 있음)를 기다리는 동안 다시 누르거나 닉네임을 저장하지 않게 막는다
        isSigningOut = true
        viewModelScope.launch {
            try {
                signOutUseCase.invokeWithTokenClear()
                loggedInUid = ""
                // 다이얼로그를 닫고 입력·동의를 모두 비운다 (다른 계정으로 다시 로그인할 수 있게)
                _uiState.update { SignInUiState() }
            } finally {
                isSigningOut = false
            }
        }
    }

    private fun sendToast(message: String, isLong: Boolean = false) {
        viewModelScope.launch { _eventChannel.send(SignInEvent.ShowToast(message, isLong)) }
    }
}
