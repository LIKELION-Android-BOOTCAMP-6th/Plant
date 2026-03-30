package com.a32b.plant.ui.feature.mypage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.data.repository.ActivityRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyCommunityFeedUistate(
    val selected: String = "",
    val activities: List<CommunityActivity> = emptyList()
)
sealed class MyCommunityFeedEvent{
    data class NavigateToCommunityDetail(val postId: String) : MyCommunityFeedEvent()
}

class MyCommunityFeedViewModel(private val repository: ActivityRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MyCommunityFeedUistate())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyCommunityFeedEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    fun onSelectedChange(type: String) {
        _uiState.update { it.copy(selected = type) }
        loadActivity(type)
    }

    fun loadActivity(selected: String){
        //셀렉티드에 맞는 활동 db에서 불러오기

        viewModelScope.launch {
            _uiState.update { it.copy(activities = repository.getActivityList(selected)) }
        }
    }

    fun moveToCommunityDetail(postId: String){
        viewModelScope.launch {
            _eventChannel.send(MyCommunityFeedEvent.NavigateToCommunityDetail(postId))
        }
    }

}