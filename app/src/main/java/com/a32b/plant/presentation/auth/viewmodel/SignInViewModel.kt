package com.a32b.plant.presentation.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
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
    val isNicknameLoading: Boolean = false
)

sealed class SignInEvent {
    data class ShowToast(val message: String) : SignInEvent()
    object NavigateToHome : SignInEvent()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val setNicknameUseCase: SetNicknameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private var loggedInUid: String = ""

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nicknameInput = value, nicknameError = null) }
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
            _uiState.update { it.copy(showNicknameDialog = true) }
        } else {
            sendToast("${nickname}님 환영합니다.")
            viewModelScope.launch { _eventChannel.send(SignInEvent.NavigateToHome) }
        }
    }

    fun confirmNickname() {
        if (_uiState.value.isNicknameLoading) return

        val nickname = _uiState.value.nicknameInput.trim()

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
                    // 네트워크 에러는 닉네임 값과 무관한 상황 에러 → 토스트, 그 외는 입력칸 인라인 에러
                    if (error is AppError.Network) {
                        _uiState.update { it.copy(isNicknameLoading = false) }
                        sendToast(error.message)
                    } else {
                        _uiState.update {
                            it.copy(nicknameError = error.message, isNicknameLoading = false)
                        }
                    }
                }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(SignInEvent.ShowToast(message)) }
    }
}
