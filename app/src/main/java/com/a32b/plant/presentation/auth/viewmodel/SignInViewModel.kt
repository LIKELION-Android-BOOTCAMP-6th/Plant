package com.a32b.plant.presentation.auth.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import com.a32b.plant.domain.util.NicknameRule
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
    data class ShowToast(val message: String, val isLong: Boolean) : SignInEvent()
    object NavigateToHome : SignInEvent()
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val setNicknameUseCase: SetNicknameUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<SignInEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    private var loggedInUid: String = ""

    init {
        loaded() // 초기에 불러올 데이터가 없으므로 LoadableScreen 로딩 없이 즉시 로그인 UI 표시
    }

    fun onNicknameChange(value: String) {
        _uiState.update {
            it.copy(
                nicknameInput = NicknameRule.limitInput(previous = it.nicknameInput, input = value),
                nicknameError = null
            )
        }
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

        val nickname = _uiState.value.nicknameInput

        // 형식 오류도 UseCase 결과로 받으므로 모든 확인 요청이 로딩 상태를 거친다.
        // 형식 오류는 UseCase가 중간에 멈추지 않고 바로 실패를 돌려줘, 이 클릭 처리 안에서 false로 돌아오므로
        // (viewModelScope는 Dispatchers.Main.immediate) 로딩 표시는 그려지지 않는다. 중복·네트워크 실패는 서버를 기다리는 동안 표시된다.
        _uiState.update { it.copy(isNicknameLoading = true, nicknameError = null) }

        viewModelScope.launch {
            setNicknameUseCase(loggedInUid, nickname)
                .onSuccess { savedNickname ->
                    _uiState.update { it.copy(showNicknameDialog = false, isNicknameLoading = false) }
                    sendToast("${savedNickname}님 환영합니다.")
                    _eventChannel.send(SignInEvent.NavigateToHome)
                }
                .onFailure { error ->
                    // 닉네임 값 자체의 문제(형식 오류·중복)만 입력칸 인라인 에러, 네트워크·저장 실패 등 상황 에러는 토스트
                    val isInputError = error is AppError.NicknameInvalid || error is AppError.NicknameTaken
                    _uiState.update {
                        it.copy(
                            nicknameError = if (isInputError) error.message else null,
                            isNicknameLoading = false
                        )
                    }
                    if (!isInputError) sendToast(error.message, isLong = error is AppError.Network)
                }
        }
    }

    private fun sendToast(message: String, isLong: Boolean = false) {
        viewModelScope.launch { _eventChannel.send(SignInEvent.ShowToast(message, isLong)) }
    }
}
