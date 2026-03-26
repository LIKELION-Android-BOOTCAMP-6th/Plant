package com.a32b.plant.ui.feature.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UI 상태
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null
)

// 일회성 이벤트
sealed class SignInEvent {
    data class ShowToast(val message: String) : SignInEvent()
    object NavigateToHome : SignInEvent()
    object NavigateToSignUp : SignInEvent()
    // 첫 로그인 → 닉네임 설정 화면 (추후 구현 시 활성화)
    // object NavigateToNicknameSetting : SignInEvent()
}

class SignInViewModel(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    // 입력 변경
    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    // 로그인 실행
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
            try {
                // 1. Firebase 로그인
                val result = auth.signInWithEmailAndPassword(state.email, state.password).await()
                val user = result.user

                if (user == null) {
                    sendToast("계정 정보가 올바르지 않습니다.")
                    return@launch
                }

                // 2. 이메일 인증 여부 확인
                if (!user.isEmailVerified) {
                    auth.signOut()
                    _uiState.update { it.copy(email = "", password = "") }
                    sendToast("이메일 인증을 진행해주세요.")
                    return@launch
                }

                // 3. Firestore에서 유저 프로필 조회 (isFirstLogin 체크)
                val profile = userRepository.getUserProfileOnce(user.uid)

                // 4. CurrentUser 싱글톤 세팅
                CurrentUser.set(
                    uid = user.uid,
                    nickname = profile?.nickname ?: "",
                    profileImg = profile?.profileImg ?: ""
                )

                // 5. 자동로그인 저장 (로그인 성공 = 자동로그인 활성화)
                userRepository.updateAutoLogin(user.uid, true)

                // 6. 첫 로그인 여부에 따라 분기
                if (profile?.isFirstLogin == true) {
                    // TODO: 닉네임 설정 화면으로 이동 (NewBornTree 등)
                    // 현재는 홈으로 이동
                    _eventChannel.send(SignInEvent.NavigateToHome)
                } else {
                    _eventChannel.send(SignInEvent.NavigateToHome)
                }

            } catch (e: Exception) {
                Log.e("SignIn", "로그인 실패: ${e.message}", e)
                handleSignInError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Firebase 에러 분기 처리
    private fun handleSignInError(e: Exception) {
        val firebaseEx = e as? FirebaseAuthException
        Log.e("SignIn", "errorCode: ${firebaseEx?.errorCode}")

        when (firebaseEx?.errorCode) {
            // 이메일 관련 오류
            "ERROR_USER_NOT_FOUND",
            "ERROR_INVALID_EMAIL" -> {
                _uiState.update { it.copy(email = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
            // 비밀번호 오류
            "ERROR_WRONG_PASSWORD" -> {
                _uiState.update { it.copy(password = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
            // 최신 Firebase SDK는 이메일/비밀번호 오류를 통합 코드로 반환
            "ERROR_INVALID_CREDENTIAL" -> {
                _uiState.update { it.copy(password = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
            else -> {
                _uiState.update { it.copy(password = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
        }
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