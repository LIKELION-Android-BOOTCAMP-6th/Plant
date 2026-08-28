package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityActivityUiState(
    val selected: String = ActivityType.POST,
    val activities: List<CommunityActivity> = emptyList()
)
sealed class CommunityActivityEvent{
    data class NavigateToCommunityDetail(val postId: String) : CommunityActivityEvent()

}
@HiltViewModel
class CommunityActivityViewModel @Inject constructor(
    private val repository: CommunityRepository
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
        _uiState.update { it.copy(selected = type) }
        loadActivity(type)
    }

    fun loadActivity(selected: String) {
        collectJob?.cancel() // 이전 구독 취소
        collectJob = viewModelScope.launch {
            repository.observeActivity(selected)
                .collect { list ->
                    _uiState.update { it.copy(activities = list) }
                }
        }
    }

    fun moveToCommunityDetail(postId: String){
        viewModelScope.launch {
            _eventChannel.send(CommunityActivityEvent.NavigateToCommunityDetail(postId))
        }
    }
}
