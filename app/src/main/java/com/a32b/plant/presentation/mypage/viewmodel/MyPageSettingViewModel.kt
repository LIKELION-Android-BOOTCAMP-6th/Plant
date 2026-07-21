package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyPageSettingViewModel @Inject constructor(
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            deleteAccountUseCase()
                .onSuccess {
                    _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                    _eventChannel.send(MyPageEvent.NavigateToSignIn)
                }
                .onFailure { error ->
                    _eventChannel.send(MyPageEvent.ShowToast(error.message))
                }
        }
    }
}
