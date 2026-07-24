package com.a32b.plant.presentation.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithEmailUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// UI 상태
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    // 닉네임 설정 다이얼로그
    val showNicknameDialog: Boolean = false,
    val nicknameInput: String = "",
    val nicknameError: String? = null,
    val isNicknameLoading: Boolean = false
)

// 일회성 이벤트
sealed class SignInEvent {
    data class ShowToast(val message: String) : SignInEvent()
    object NavigateToHome : SignInEvent()
    object NavigateToSignUp : SignInEvent()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val setNicknameUseCase: SetNicknameUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    // 로그인 성공 후 저장해두는 uid (닉네임 설정 시 사용)
    private var loggedInUid: String = ""

    // 입력 변경
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nicknameInput = value, nicknameError = null) }
    }

    // 이메일 로그인 실행
    fun signIn() {
        val state = _uiState.value

        // 에러 초기화
        _uiState.update { it.copy(emailError = null) }

        // 빈칸 검증
        if (state.email.isBlank() || state.password.isBlank()) {
            sendToast("이메일과 비밀번호를 입력해주세요.")
            return
        }

        // 이메일 형식 검증
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "이메일 형식이 올바르지 않습니다.") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            signInWithEmailUseCase(state.email, state.password)
                .onSuccess { result -> handleLoginSuccess(result.uid, result.nickname, result.isFirstLogin) }
                .onFailure { error -> handleSignInError(error) }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // 구글 로그인
    // SignInScreen에서 구글 계정 선택 후 받은 idToken을 여기로 전달
    fun handleGoogleSignIn(idToken: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { result -> handleLoginSuccess(result.uid, result.nickname, result.isFirstLogin) }
                .onFailure { error ->
                    Log.e("SignIn", "구글 로그인 실패: ${error.message}")
                    sendToast("구글 로그인 실패: ${error.message}")
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // 이메일/구글 로그인 공통 처리 — 첫 로그인 여부에 따라 분기
    private fun handleLoginSuccess(uid: String, nickname: String, isFirstLogin: Boolean) {
        loggedInUid = uid

        if (isFirstLogin) {
            _uiState.update { it.copy(showNicknameDialog = true) }
        } else {
            sendToast("${nickname}님 환영합니다.")
            viewModelScope.launch { _eventChannel.send(SignInEvent.NavigateToHome) }
        }
    }

    // 닉네임 설정
    fun confirmNickname() {
        val nickname = _uiState.value.nicknameInput.trim()

        // 글자수 검증 (2~10자)
        if (nickname.length !in 2..10) {
            _uiState.update { it.copy(nicknameError = "2자 이상 10자 이하로 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isNicknameLoading = true, nicknameError = null) }

        viewModelScope.launch {
            setNicknameUseCase(loggedInUid, nickname)
                .onSuccess {
                    _uiState.update { it.copy(showNicknameDialog = false, isNicknameLoading = false) }
                    sendToast("${nickname}님 환영합니다.")
                    _eventChannel.send(SignInEvent.NavigateToHome)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(nicknameError = error.message, isNicknameLoading = false)
                    }
                }
        }
    }

    // 비밀번호 재설정 메일 전송
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess { sendToast("재설정 메일을 전송했습니다.") }
                .onFailure { sendToast("메일 전송에 실패했습니다.\n이메일을 확인해주세요.") }
        }
    }

    // 로그인 실패 분기 처리
    private fun handleSignInError(error: AppError) {
        when (error) {
            is AppError.Email -> _uiState.update { it.copy(email = "") }
            is AppError.Auth -> _uiState.update { it.copy(password = "") }
            else -> _uiState.update { it.copy(email = "", password = "") }
        }
        sendToast(error.message)
    }

    // 이메일 형식 검증
    private fun isValidEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email)
    }

    // 토스트 전송
    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(SignInEvent.ShowToast(message)) }
    }
}
