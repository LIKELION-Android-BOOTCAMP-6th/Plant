package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val showPasswordDialog: Boolean = false
)

sealed class DeleteAccountEvent {
    data class ShowToast(val message: String) : DeleteAccountEvent()
    object NavigateToSignIn : DeleteAccountEvent()
    object RequestGoogleReauth : DeleteAccountEvent()
}

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<DeleteAccountEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    /** 2단계 확인 완료 후 로딩 시작 → 로그인 제공자에 따라 재인증 방식 분기 */
    fun requestDeleteAccount() {
        val provider = authRepository.getSignInProvider()
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            if (provider == "google.com") {
                _eventChannel.send(DeleteAccountEvent.RequestGoogleReauth)
            } else {
                // 이메일: 비밀번호 다이얼로그가 즉시 뜨므로 로딩 해제
                _uiState.update { it.copy(showPasswordDialog = true, isLoading = false) }
            }
        }
    }

    fun cancelLoading() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun dismissPasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false) }
    }

    /** 이메일 유저: 비밀번호로 재인증 후 삭제 */
    fun reauthenticateWithEmailAndDelete(password: String) {
        val email = authRepository.currentEmail() ?: return
        _uiState.update { it.copy(showPasswordDialog = false, isLoading = true) }
        viewModelScope.launch {
            authRepository.reauthenticateWithEmail(email, password)
                .onSuccess { performDelete() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (error !is AppError.UnknownUser) {
                        _eventChannel.send(DeleteAccountEvent.ShowToast(error.message))
                    }
                }
        }
    }

    /** 구글 유저: idToken으로 재인증 후 삭제 (idToken은 Screen에서 Credential Manager로 획득) */
    fun reauthenticateWithGoogleAndDelete(idToken: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.reauthenticateWithGoogle(idToken)
                .onSuccess { performDelete() }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    if (error !is AppError.UnknownUser) {
                        _eventChannel.send(DeleteAccountEvent.ShowToast(error.message))
                    }
                }
        }
    }

    private suspend fun performDelete() {
        deleteAccountUseCase()
            .onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                _eventChannel.send(DeleteAccountEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                _eventChannel.send(DeleteAccountEvent.NavigateToSignIn)
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                if (error !is AppError.UnknownUser) {
                    _eventChannel.send(DeleteAccountEvent.ShowToast(error.message))
                }
            }
    }
}
