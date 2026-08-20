package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPageSettingUiState(
    val isLoading: Boolean = false
)

@HiltViewModel
class MyPageSettingViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageSettingUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun deleteAccount() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            deleteAccountUseCase()
                .onSuccess {
                    _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                    _eventChannel.send(MyPageEvent.NavigateToSignIn)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (error) {
                        is AppError.UnknownUser -> ensureCurrentUserUseCase()
                        else -> _eventChannel.send(MyPageEvent.ShowToast(error.message))
                    }
                }
        }
    }
}
