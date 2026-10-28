package com.a32b.plant.presentation.community.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.DeleteActivitiesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityActivityUiState(
    val isLoading: Boolean = false,
    val selected: String = ActivityType.POST,
    val activities: List<CommunityActivity> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val isDeleteDialogShown: Boolean = false
) {
    // 화면에 보이는 항목 중 선택된 개수. selectedIds를 직접 깎지 않는 이유:
    // Firestore 삭제는 로컬에서 먼저 반영되므로, 서버가 거절해 항목이 다시 나타나도 선택이 유지되어야 함
    val selectedCount: Int
        get() = activities.count { it.id in selectedIds }
}
sealed class CommunityActivityEvent {
    data class NavigateToCommunityDetail(val postId: String) : CommunityActivityEvent()
    data class ShowToast(val message: String) : CommunityActivityEvent()
}
@HiltViewModel
class CommunityActivityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val deleteActivitiesUseCase: DeleteActivitiesUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(CommunityActivityUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityActivityEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private var collectJob: Job? = null
    init {
        loadActivity(_uiState.value.selected)
    }
    fun onSelectedChange(type: String) {
        _uiState.update { it.copy(selected = type, isSelectionMode = false, selectedIds = emptySet()) }
        loadActivity(type)
    }

    fun loadActivity(selected: String) {
        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.observeActivity(selected)
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, activities = list) }
                    loaded() // BaseViewModel: 최초 방출 시 LoadableScreen 해제 (탭 전환 로딩은 LoadingBox)
                }
            // 구독 실패로 방출 없이 Flow가 끝난 경우에도 로딩 해제 → 빈 상태 UI 표시
            _uiState.update { it.copy(isLoading = false) }
            loaded()
        }
    }

    fun moveToCommunityDetail(postId: String) {
        viewModelScope.launch {
            _eventChannel.send(CommunityActivityEvent.NavigateToCommunityDetail(postId))
        }
    }

    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
    }

    fun toggleItemSelection(id: String) {
        _uiState.update { state ->
            val newIds = if (id in state.selectedIds) state.selectedIds - id
                         else state.selectedIds + id
            state.copy(selectedIds = newIds)
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(isDeleteDialogShown = true) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(isDeleteDialogShown = false) }
    }

    fun deleteSelected() {
        val toDelete = _uiState.value.activities.filter { it.id in _uiState.value.selectedIds }
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteActivitiesUseCase(toDelete)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isSelectionMode = false, selectedIds = emptySet()) }
                    sendToast("삭제되었습니다.")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (e) {
                        is AppError.UnknownUser -> Unit // UseCase 내부 ensureCurrentUserUseCase에서 이미 세션 만료 알림 발송
                        else -> sendToast(e.message)
                    }
                }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityActivityEvent.ShowToast(message)) }
    }
}
