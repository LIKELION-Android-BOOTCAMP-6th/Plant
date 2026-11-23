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
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommunityActivityUiState(
    // 탭 목록을 불러오는 중. 목록이 오거나 구독이 끝나면 해제된다
    val isLoading: Boolean = false,
    // 선택 항목을 삭제하는 중. 삭제 결과가 나와야 해제된다. 화면은 이 동안 상단바까지 덮어 터치를 막고 TalkBack 실행도 막는다 (#753)
    // isLoading과 따로 두는 이유: 삭제한 activity가 구독 중인 목록에서 빠지며 목록이 삭제 도중에 다시 오므로,
    // 같은 값을 쓰면 목록 도착이 삭제 중 로딩을 풀고, 삭제 종료가 다른 탭의 로딩을 푼다 (#754)
    val isDeleting: Boolean = false,
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

    // 선택 모드 해제만 한다 (상단바 "취소"·시스템 뒤로가기). 뒤집기가 아니라서 이미 꺼진 상태에서 불려도 다시 켜지지 않는다 (#748)
    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedIds = emptySet()) }
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
        // 삭제가 겹쳐 시작되면 먼저 끝난 쪽이 isDeleting을 풀어 버리므로, 진행 중이면 새로 시작하지 않는다 (#754)
        if (toDelete.isEmpty() || _uiState.value.isDeleting) return

        // 위의 진행 중 확인과 같은 흐름에서 바로 켜지도록 launch 밖에 둔다
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            // NonCancellable: 응답 전에 화면을 나가도 남은 항목을 끝까지 처리하고, 성공한 묶음마다 목록에 알리기 위함 (#712)
            // 목록 알림은 UseCase 안에서 보낸다. 이탈 후에도 블록 밖 상태 갱신은 실행되지만(NonCancellable은 dispatcher를 바꾸지 않고
            // UseCase가 자식 코루틴을 모두 끝낸 뒤 돌아오므로, withContext가 끝날 때 바깥 취소를 확인하지 않음)
            // 화면이 없어 영향이 없고, Toast는 취소된 viewModelScope의 launch라 나가지 않는다
            withContext(NonCancellable) { deleteActivitiesUseCase(toDelete) }
                .onSuccess {
                    _uiState.update { it.copy(isDeleting = false, isSelectionMode = false, selectedIds = emptySet()) }
                    sendToast("삭제되었습니다.")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isDeleting = false) }
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
