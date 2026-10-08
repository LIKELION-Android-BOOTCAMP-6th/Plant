package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
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
    val isDeletingActivities: Boolean = false
)
sealed class CommunityActivityEvent {
    data class NavigateToCommunityDetail(val postId: String) : CommunityActivityEvent()
    data class ShowToast(val message: String) : CommunityActivityEvent()
}
@HiltViewModel
class CommunityActivityViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val deleteActivitiesUseCase: DeleteActivitiesUseCase
) : ViewModel() {
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
                }
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

    fun deleteSelected() {
        val toDelete = _uiState.value.activities.filter { it.id in _uiState.value.selectedIds }
        if (toDelete.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingActivities = true) }
            deleteActivitiesUseCase(toDelete)
                .onSuccess {
                    _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
                    sendToast("삭제되었습니다.")
                }
                .onFailure { e ->
                    sendToast(e.message ?: "삭제에 실패했습니다.")
                }
            _uiState.update { it.copy(isDeletingActivities = false) }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityActivityEvent.ShowToast(message)) }
    }
}
