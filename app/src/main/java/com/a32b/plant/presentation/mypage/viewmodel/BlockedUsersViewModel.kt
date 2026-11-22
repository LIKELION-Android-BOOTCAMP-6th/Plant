package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.ObserveBlockedUsersUseCase
import com.a32b.plant.domain.usecase.community.UnblockUserUseCase
import com.a32b.plant.presentation.community.CommunityConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedUsersUiState(
    val isLoading: Boolean = false,      // 차단 해제 처리 중
    val users: List<BlockedUser> = emptyList(),
    val unblockTarget: BlockedUser? = null  // 해제 확인 다이얼로그 대상, null이면 닫힘
)

sealed class BlockedUsersEvent {
    data class ShowToast(val message: String) : BlockedUsersEvent()
}

/** 마이페이지 > 차단 목록 (#660) */
@HiltViewModel
class BlockedUsersViewModel @Inject constructor(
    private val observeBlockedUsersUseCase: ObserveBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<BlockedUsersEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    companion object {
        // 차단 목록이 오지 않아도(리스너 응답 없음) 로딩 화면에 머물지 않도록 기다리는 최대 시간. 첫 목록 전에 리스너가 실패하면 저장소가 빈 목록을 보낸다
        private const val INITIAL_WAIT_MS = 2_000L
        private const val UNBLOCKED_MESSAGE = "차단을 해제했어요"
    }

    init {
        observeBlockedUsers()
        viewModelScope.launch {
            delay(INITIAL_WAIT_MS)
            loaded()
        }
    }

    // 해제하면 구독 중인 목록에서 바로 빠진다 (오프라인이어도 기기 캐시에 먼저 반영)
    private fun observeBlockedUsers() {
        viewModelScope.launch {
            observeBlockedUsersUseCase().collect { users ->
                _uiState.update { state ->
                    // 해제 확인 중인 사용자가 다른 기기에서 먼저 해제됐으면 다이얼로그를 닫는다
                    val target = state.unblockTarget?.takeIf { t -> users.any { it.uid == t.uid } }
                    state.copy(users = users, unblockTarget = target)
                }
                loaded()
            }
        }
    }

    fun openUnblockDialog(user: BlockedUser) {
        // 해제 처리 중에는 열지 않는다 — 해제하면 목록에서 바로 빠져 확인 창이 먼저 닫히므로, 처리 중(오프라인이면 최대 5초)에
        // 다른 사용자의 창이 로딩 상태로 열려 닫을 수 없게 되는 것을 막는다
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(unblockTarget = user) }
    }

    fun closeUnblockDialog() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(unblockTarget = null) }
    }

    fun confirmUnblock() {
        val state = _uiState.value
        val target = state.unblockTarget ?: return
        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            unblockUserUseCase(target.uid)
                .onSuccess { saveResult ->
                    _uiState.update { it.copy(isLoading = false, unblockTarget = null) }
                    sendToast(if (saveResult == SaveResult.Saved) UNBLOCKED_MESSAGE else CommunityConstants.WRITE_PENDING_MESSAGE)
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (e) {
                        // 세션 만료 안내는 UseCase의 유저 확인에서 이미 보냈다
                        is AppError.UnknownUser -> _uiState.update { it.copy(unblockTarget = null) }
                        else -> sendToast(e.message)
                    }
                }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _event.send(BlockedUsersEvent.ShowToast(message)) }
    }
}
