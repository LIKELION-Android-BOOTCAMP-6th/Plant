package com.a32b.plant.ui.feature.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

class SignUpViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
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

        //state event
        viewModelScope.launch {
            try {
                // 1. Firebase Auth 계정 생성
                val uid = createFirebaseUser(state.email, state.password)

                // 2. 인증 메일 발송
                sendVerificationEmail()

                // uid SignInViewModel에 넘기기?
                userRepository.createUser(uid).getOrThrow()

                // 3. 이메일 인증 전까지 로그인 차단
                auth.signOut()

                sendToast("회원가입 완료! 인증 메일을 확인해주세요.")
                _eventChannel.send(SignUpEvent.NavigateToSignIn)

            } catch (e: Exception) {
                // Firebase 오류 코드 로그로 확인
                 val firebaseEx = e as? com.google.firebase.auth.FirebaseAuthException
                 Log.e("AuthError", "Code: ${firebaseEx?.errorCode}, Msg: ${e.message}")

                val message = when (firebaseEx?.errorCode) {
                    // Firebase에서 돌아오는 오류 코드로 분기
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "이미 등록된 계정입니다."
                    "ERROR_WEAK_PASSWORD"        -> "비밀번호 조건을 맞춰주세요."
                    "ERROR_INVALID_EMAIL"        -> "이메일 형식이 올바르지 않습니다."
                    else                         -> "회원가입 실패. 다시 시도해주세요."
                }
                sendToast(message)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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

    // Firebase Auth 계정 생성 - suspend 함수
    private suspend fun createFirebaseUser(email: String, password: String): String =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid
                    if (uid != null) cont.resume(uid)
                    else cont.resumeWithException(Exception("uid를 가져오지 못했습니다."))
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    // 인증 메일 발송 - suspend 함수
    private suspend fun sendVerificationEmail() =
        suspendCancellableCoroutine { cont ->
            auth.currentUser
                ?.sendEmailVerification()
                ?.addOnSuccessListener { cont.resume(Unit) }
                ?.addOnFailureListener { e -> cont.resumeWithException(e) }
                ?: cont.resumeWithException(Exception("현재 유저가 없습니다."))
        }

    // 토스트 함수 공통소스화?
    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(SignUpEvent.ShowToast(message)) }
    }

}