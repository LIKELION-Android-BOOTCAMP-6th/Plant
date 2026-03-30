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
import com.google.firebase.auth.GoogleAuthProvider

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
            try {
                // 1. Firebase 로그인
                // 상세 설명: 아래 코드 한 줄이 동작하는 순간 Firebase SDK가 내부적으로 기기 로컬 저장소에 인증 토큰을 자동 저장
                // auth.currentUser에 정보 저장됨 -> 이후 앱 껏다 켜도 로그인된 유저 정보 반환
                val result = auth.signInWithEmailAndPassword(state.email, state.password).await()
                val user = result.user

                // 컴파일러 통과용 방어 코드
                // result.user의 타입이 FirebaseUser?라서 nullable. null 체크 없이 아래에서 user.isEmailVerified, user.uid 같은 걸 쓰면 Kotlin 컴파일러가 빌드를 안 시켜줌.
                if (user == null) {
                    sendToast("계정 정보가 올바르지 않습니다.")
                    return@launch
                }

                // 2. 이메일 인증 여부 확인
                if (!user.isEmailVerified) {
                    auth.signOut()
                    _uiState.update { it.copy(email = "", password = "") }
                    sendToast("이메일을 인증해주세요.")
                    return@launch
                }

                // 이메일 로그인 성공 → 공통 프로필 처리
                handleLoginSuccess(user.uid)


            } catch (e: Exception) {
                Log.e("SignIn", "로그인 실패: ${e.message}", e)
                handleSignInError(e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    // 구글 로그인
    // SignInScreen에서 구글 계정 선택 후 받은 idToken을 여기로 전달
    fun handleGoogleSignIn(idToken: String) {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // 1. Google idToken → Firebase 인증 정보로 변환
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                // 2. Firebase Auth에 구글 계정으로 로그인
                val result = auth.signInWithCredential(credential).await()
                val user = result.user

                if (user == null) {
                    sendToast("잘못된 접근입니다.")
                    return@launch
                }

                // 3. 구글 로그인 성공 → 공통 프로필 처리
                // (이메일 인증 체크 불필요 — 구글 계정은 이미 인증된 상태)
                handleLoginSuccess(user.uid)

            } catch (e: Exception) {
                Log.e("SignIn", "구글 로그인 실패: ${e.message}", e)
                sendToast("잘못된 접근입니다.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // 추가: 이메일/구글 로그인 공통 처리
    // 로그인 성공 후 Firestore 프로필 확인 → 없으면 생성 → 닉네임 설정 or 홈 진입
    private suspend fun handleLoginSuccess(uid: String) {
        loggedInUid = uid

        // 1. Firestore에 유저 문서가 있는지 확인 → 없으면 생성
        var profile = userRepository.getUserProfileOnce(uid)

        if (profile == null) {
            val createResult = userRepository.createUser(uid)
            if (createResult.isFailure) {
                sendToast("정보 생성에 실패했습니다.\n다시 시도해주세요.")
                auth.signOut()
                return
            }
            profile = userRepository.getUserProfileOnce(uid)
        }

        // 2. CurrentUser 싱글톤 세팅
        CurrentUser.set(
            uid = uid,
            nickname = profile?.nickname ?: "",
            profileImg = profile?.profileImg ?: ""
        )

        // 3. 첫 로그인 여부에 따라 분기
        if (profile?.isFirstLogin == true) {
            _uiState.update { it.copy(showNicknameDialog = true) }
        } else {
            sendToast("${CurrentUser.nickname}님 환영합니다.")
            _eventChannel.send(SignInEvent.NavigateToHome)
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
            try {
                // 1. 닉네임 중복 검사
                val isDuplicate = nicknameRepository.isNicknameTaken(nickname)
                if (isDuplicate) {
                    _uiState.update {
                        it.copy(nicknameError = "사용 중인 닉네임입니다.", isNicknameLoading = false)
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
                sendToast("${CurrentUser.nickname}님 환영합니다.")
                _eventChannel.send(SignInEvent.NavigateToHome)

            } catch (e: Exception) {
                Log.e("SignIn", "닉네임 설정 실패: ${e.message}", e)
                _uiState.update {
                    it.copy(nicknameError = "닉네임 설정에 실패했습니다.", isNicknameLoading = false)
                }
            }
        }
    }


    // 비밀번호 재설정 메일 전송
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                sendToast("재설정 메일을 전송했습니다.")
            } catch (e: Exception) {
                Log.e("SignIn", "비밀번호 재설정 메일 전송 실패: ${e.message}", e)
                sendToast("메일 전송에 실패했습니다.\n이메일을 확인해주세요.")
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
                sendToast("이메일 형식을 확인해주세요")
            }
            // 가입 정보 없음, 비밀번호 오류 -> '계정 정보가 올바르지 않습니다.' 로 통합
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD" -> {
                _uiState.update { it.copy(password = "") }
                sendToast("계정 정보가 올바르지 않습니다.")
            }
            // 앱 인증 (토큰 검증) 실패
            "ERROR_INVALID_CREDENTIAL" -> {
                _uiState.update {
                    it.copy(
                        email = "",
                        password = ""
                    )
                }
                sendToast("로그인에 실패했습니다. \n다시 시도해주세요.")
            }

            else -> {
                _uiState.update {
                    it.copy(
                        email = "",
                        password = ""
                    )
                }
                sendToast("로그인에 실패했습니다.\n다시 시도해주세요.")
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