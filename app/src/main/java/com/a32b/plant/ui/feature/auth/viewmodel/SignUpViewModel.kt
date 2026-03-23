package com.a32b.plant.ui.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.a32b.plant.data.di.AppContainer
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
    val agreePrivacy: Boolean = false,
    val agreeTerms: Boolean = false,
    val isLoading: Boolean = false,
    val passwordError: String? = null
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
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }
    fun onPasswordConfirmChange(value: String) = _uiState.update { it.copy(passwordConfirm = value, passwordError = null) }
    fun onAgreePrivacyChange(value: Boolean) = _uiState.update { it.copy(agreePrivacy = value) }
    fun onAgreeTermsChange(value: Boolean) = _uiState.update { it.copy(agreeTerms = value) }

    fun signUp() {
        val state = _uiState.value

        // 미입력 항목 검증
        if (state.email.isBlank() || state.password.isBlank() ||
            state.passwordConfirm.isBlank() || !state.agreePrivacy || !state.agreeTerms
        ) {
            sendToast("모든 항목을 작성해주세요.")
            return
        }

        // 비밀번호 일치 검증
        if (state.password != state.passwordConfirm) {
            _uiState.update { it.copy(passwordError = "비밀번호가 일치하지 않습니다.") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // 1. Firebase Auth 계정 생성
                val uid = createFirebaseUser(state.email, state.password)

                // 2. 인증 메일 발송
                sendVerificationEmail()

                // 3. Firestore users 문서 생성 (isFirstLogin = true)
                userRepository.createUser(uid).getOrThrow()

                // 4. 이메일 인증 전까지 로그인 차단
                auth.signOut()

                sendToast("회원가입 성공! 전송된 이메일을 확인해주세요.")
                _eventChannel.send(SignUpEvent.NavigateToSignIn)

            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("email address is already in use") == true ->
                        "이미 등록된 계정입니다."
                    else -> "회원가입 실패. 다시 시도해주세요."
                }
                sendToast(message)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
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

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(SignUpEvent.ShowToast(message)) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SignUpViewModel(
                    auth = AppContainer.firebaseAuth,
                    userRepository = AppContainer.userRepository
                )
            }
        }
    }
}