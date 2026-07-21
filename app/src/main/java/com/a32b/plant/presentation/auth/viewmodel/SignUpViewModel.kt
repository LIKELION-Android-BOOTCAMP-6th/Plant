package com.a32b.plant.presentation.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.SignUpWithEmailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val passwordConfirm: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordConfirmError: String? = null
)

sealed class SignUpEvent {
    data class ShowToast(val message: String) : SignUpEvent()
    object NavigateToSignIn : SignUpEvent()
}
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignUpEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }
    fun onPasswordConfirmChange(value: String) = _uiState.update { it.copy(passwordConfirm = value) }

    fun signUp() {
        val state = _uiState.value

        // 회원가입 버튼 클릭 시 기존 3종 검증 에러(이메일 형식 검증, 비밀번호 조건 검증, 비밀번호 일치 검증) 전부 초기화
        _uiState.update { it.copy(
            emailError = null,
            passwordError = null,
            passwordConfirmError = null
        )}

        // 미입력 항목 검증
        if (state.email.isBlank() || state.password.isBlank() || state.passwordConfirm.isBlank()) {
            sendToast("모든 항목을 작성해주세요.")
            return
        }

        // 이메일, 비밀번호 조건, 비밀번호 일치 3가지 검증을 한번에 실행
        var hasError = false

        // 이메일 형식 검증
        if (!isValidEmail(state.email)) {
            _uiState.update { it.copy(emailError = "이메일 형식이 올바르지 않습니다.") }
            hasError = true
        }

        // 비밀번호 조건 검증 (소문자 + 숫자 + 특수문자, 6자리 이상)
        if (!isValidPassword(state.password)) {
            _uiState.update { it.copy(passwordError = "비밀번호 조건을 맞춰주세요.") }
            hasError = true
        }

        // 비밀번호 일치 검증
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(passwordConfirmError = "비밀번호가 일치하지 않습니다.") }
            hasError = true
        }

        // 에러가 하나라도 있으면 진행 중단
        if (hasError) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            signUpWithEmailUseCase(state.email, state.password)
                .onSuccess {
                    sendToast("회원가입 완료! 인증 메일을 확인해주세요.")
                    _eventChannel.send(SignUpEvent.NavigateToSignIn)
                }
                .onFailure { error ->
                    Log.e("SignUp", "회원가입 실패: ${error.message}")
                    sendToast(error.message)
                }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // 비밀번호 조건 검증 함수
    // 소문자, 숫자, 특수문자 포함 6자리 이상
    private fun isValidPassword(password: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[0-9])(?=.*[!@#\$%^&*()_+\\-=]).{6,}$")
        return regex.matches(password)
    }

    // 이메일 형식 검증 함수
    private fun isValidEmail(email: String): Boolean {
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return regex.matches(email)
    }

    // 토스트 함수
    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(SignUpEvent.ShowToast(message)) }
    }

}
