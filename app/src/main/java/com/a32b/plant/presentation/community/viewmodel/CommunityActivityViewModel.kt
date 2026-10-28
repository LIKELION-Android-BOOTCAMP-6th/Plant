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
)
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
                    // 일부 삭제 실패 후 선택 모드가 유지될 때, 이미 지워진 항목이 선택 개수에 남지 않도록 정리
                    _uiState.update { state ->
                        val ids = list.map { it.id }.toSet()
                        state.copy(isLoading = false, activities = list, selectedIds = state.selectedIds intersect ids)
                    }
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
                    // 세션 만료는 ensureCurrentUserUseCase에서 이미 알림을 보냈으므로 토스트 생략
                    if (e !is AppError.UnknownUser) sendToast(e.message)
                }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityActivityEvent.ShowToast(message)) }
    }
}
