package com.a32b.plant.ui.feature.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.NicknameRepository
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

class SignInViewModel(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    private val nicknameRepository: NicknameRepository
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

                loggedInUid = user.uid


                // 3. Firestore에 유저 문서가 있는지 확인 → 없으면 생성
                var profile = userRepository.getUserProfileOnce(user.uid)

                if (profile == null) {
                    // 이메일 인증 후 첫 로그인 → Firestore에 유저 데이터 생성
                    val createResult = userRepository.createUser(user.uid)
                    if (createResult.isFailure) {
                        sendToast("유저 정보 생성에 실패했습니다. 다시 시도해주세요.")
                        auth.signOut()
                        return@launch
                    }
                    // 생성 직후 다시 조회
                    profile = userRepository.getUserProfileOnce(user.uid)
                }

                // 4. CurrentUser 싱글톤 세팅
                CurrentUser.set(
                    uid = user.uid,
                    nickname = profile?.nickname ?: "",
                    profileImg = profile?.profileImg ?: ""
                )

                // 5. 첫 로그인 여부에 따라 분기
                if (profile?.isFirstLogin == true) {
                    // 닉네임 설정 다이얼로그 표시
                    _uiState.update { it.copy(showNicknameDialog = true) }
                } else {
                    // 자동로그인 저장 후 홈 진입
                    userRepository.updateAutoLogin(user.uid, true)
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

    // 닉네임 설정
    fun confirmNickname() {
        val nickname = _uiState.value.nicknameInput.trim()

        // 글자수 검증 (2~10자)
        if (nickname.length < 2 || nickname.length > 10) {
            _uiState.update { it.copy(nicknameError = "닉네임은 2자 이상 10자 이하로 입력해주세요.") }
            return
        }

        _uiState.update { it.copy(isNicknameLoading = true, nicknameError = null) }

        viewModelScope.launch {
            try {
                // 1. 닉네임 중복 검사
                val isDuplicate = nicknameRepository.isNicknameTaken(nickname)
                if (isDuplicate) {
                    _uiState.update {
                        it.copy(nicknameError = "이미 사용 중인 닉네임입니다.", isNicknameLoading = false)
                    }
                    return@launch
                }

                // 2. nicknames 컬렉션에 닉네임 등록
                nicknameRepository.registerNickname(nickname)

                // 3. users/{uid} 문서에 닉네임 저장 + isFirstLogin → false + isAutoLogin → true
                userRepository.completeFirstLogin(loggedInUid, nickname)

                // 4. CurrentUser 업데이트
                CurrentUser.nickname = nickname

                // 5. 다이얼로그 닫고 홈으로 이동
                _uiState.update { it.copy(showNicknameDialog = false, isNicknameLoading = false) }
                _eventChannel.send(SignInEvent.NavigateToHome)

            } catch (e: Exception) {
                Log.e("SignIn", "닉네임 설정 실패: ${e.message}", e)
                _uiState.update {
                    it.copy(nicknameError = "닉네임 설정에 실패했습니다.", isNicknameLoading = false)
                }
            }
        }
    }


    // Firebase 에러 분기 처리
    private fun handleSignInError(e: Exception) {
        val firebaseEx = e as? FirebaseAuthException
        Log.e("SignIn", "errorCode: ${firebaseEx?.errorCode}")

        when (firebaseEx?.errorCode) {
            // 이메일 형식 오류
            "ERROR_INVALID_EMAIL" -> {
                _uiState.update { it.copy(email = "") }
                sendToast("바른 이메일 형식을 입력해주세요")
            }
            // 가입 정보 없음, 비밀번호 오류 -> '계정 정보가 올바르지 않습니다.' 로 통합
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD" -> {
                _uiState.update { it.copy(password = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
            // 앱 인증 (토큰 검증) 실패
            "ERROR_INVALID_CREDENTIAL" -> {
                _uiState.update { it.copy(password = "") }
                sendToast("로그인에 실패했습니다. 다시 시도해주세요.")
            }
            else -> {
                _uiState.update { it.copy(password = "") }
                sendToast("로그인에 실패했습니다. 다시 시도해주세요.")
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