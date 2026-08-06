package com.a32b.plant.presentation.community.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.domain.usecase.community.DeleteCommentUseCase
import com.a32b.plant.domain.usecase.community.DeletePostUseCase
import com.a32b.plant.domain.usecase.community.ObserveCommentsUseCase
import com.a32b.plant.domain.usecase.community.ObservePostDetailUseCase
import com.a32b.plant.domain.usecase.community.ToggleLikeUseCase
import com.a32b.plant.domain.usecase.community.UpdateCommentUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class CommunityDetailUiState(
    val comment: String = "",
    val commentList: List<Comment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isShared: Boolean = false,
    val studyLogs: List<StudyLog>? = emptyList(),
    val currentUid: String = "",
    val currentNickname: String = "",
    val isCommentSubmitting: Boolean = false,

    // 댓글 수정 상태
    val editingCommentId: String? = null,
    val editingCommentText: String = "",

    // 댓글 삭제 대상 ID
    val deletingCommentId: String? = null
)

sealed class CommunityDetailEvent {
    data class ShowToast(val message: String) : CommunityDetailEvent()
    object NavigateBack : CommunityDetailEvent()
}

@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    private val observePostDetailUseCase: ObservePostDetailUseCase,
    private val observeCommentsUseCase: ObserveCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val updateCommentUseCase: UpdateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityDetailEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    val postId: String = savedStateHandle.get<String>("postId") ?: ""
    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()


    private val _isLikeProcessing = MutableStateFlow(false)
    val isLikeProcessing: StateFlow<Boolean> = _isLikeProcessing.asStateFlow()

    init {
        ensureCurrentUserUseCase { user -> _uiState.update { it.copy(currentUid = user.uid, currentNickname = user.nickname) } }
        loadPostDetail()
        observeComments()
        onIsSharedChange()
    }

    private fun loadPostDetail() {
        observePostDetailUseCase(postId).onEach { _post.value = it }.launchIn(viewModelScope)
    }

    //공유 여부 체크
    fun onIsSharedChange(){
        if (!_post.value?.studyLogs.isNullOrEmpty())
            _uiState.update { it.copy(isShared = true) }
    }

    // 댓글 실시간 구독: 로컬에 쓴 즉시(오프라인이어도) 반영되므로 addComment 등에서 별도로 다시 불러올 필요 없음
    private fun observeComments() {
        observeCommentsUseCase(postId)
            .catch { e -> sendToast(e.message ?: "댓글을 불러오지 못했습니다.") }
            .onEach { comments -> _uiState.update { it.copy(commentList = comments) } }
            .launchIn(viewModelScope)
    }

    fun onCommentChange(newText: String) { _uiState.update { it.copy(comment = newText) } }

    fun openDeleteDialog() { _showDeleteDialog.value = true }
    fun closeDeleteDialog() { _showDeleteDialog.value = false }

    fun addComment() {

        if (_uiState.value.comment.isBlank() || _uiState.value.isCommentSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSubmitting = true) }

            runWithSlowNotice { addCommentUseCase(postId, _post.value?.title ?: "", _uiState.value.comment) }
                .onSuccess {
                    _uiState.update { it.copy(comment = "") }
                }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }

            _uiState.update { it.copy(isCommentSubmitting = false) }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            runWithSlowNotice { deletePostUseCase(postId) }
                .onSuccess { _eventChannel.send(CommunityDetailEvent.NavigateBack) }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            closeDeleteDialog()
        }
    }

    fun toggleLike() {
        val currentPost = _post.value ?: return

        if (currentPost.author.id == _uiState.value.currentUid) return

        if (_isLikeProcessing.value) return

        viewModelScope.launch {
            _isLikeProcessing.value = true
            runWithSlowNotice { toggleLikeUseCase(postId, currentPost.author.id, currentPost.isLiked, currentPost.title) }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            _isLikeProcessing.value = false
        }
    }

    // 댓글 수정 관련 함수
    fun startEditComment(comment: Comment) {
        _uiState.update {
            it.copy(editingCommentId = comment.commentId, editingCommentText = comment.content)
        }
    }

    // 댓글 수정 관련 함수
    fun onEditCommentTextChange(text: String) {
        _uiState.update { it.copy(editingCommentText = text) }
    }

    // 댓글 수정 관련 함수
    fun cancelEditComment() {
        _uiState.update { it.copy(editingCommentId = null, editingCommentText = "") }
    }

    // 댓글 수정 관련 함수
    fun submitEditComment() {
        val state = _uiState.value
        val commentId = state.editingCommentId ?: return
        if (state.editingCommentText.isBlank()) return

        viewModelScope.launch {
            runWithSlowNotice { updateCommentUseCase(postId, commentId, state.editingCommentText) }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            _uiState.update { it.copy(editingCommentId = null, editingCommentText = "") }
        }
    }

    // 댓글 삭제 관련 함수
    fun openCommentDeleteDialog(commentId: String) {
        _uiState.update { it.copy(deletingCommentId = commentId) }
    }

    // 댓글 삭제 관련 함수
    fun closeCommentDeleteDialog() {
        _uiState.update { it.copy(deletingCommentId = null) }
    }

    // 댓글 삭제 관련 함수
    fun deleteComment() {
        val commentId = _uiState.value.deletingCommentId ?: return

        viewModelScope.launch {
            runWithSlowNotice { deleteCommentUseCase(postId, commentId) }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            _uiState.update { it.copy(deletingCommentId = null) }
        }
    }

    /**
     * Firestore 쓰기 작업(set/update/runBatch)은 오프라인이어도 로컬 큐에 즉시 등록되고,
     * 재연결 시 그대로 서버에 반영된다. 즉 코루틴을 타임아웃으로 "포기"해도 이미 큐잉된 쓰기 자체는
     * 취소되지 않는다 — 여기서 진짜로 취소해버리면 "실패했다"는 잘못된 신호를 주고 사용자가 재시도하게
     * 만들어 중복 쓰기가 쌓인다.
     *
     * 그래서 작업은 끝까지 실행되도록 두고, [NETWORK_SLOW_MESSAGE_DELAY]가 지나도 끝나지 않으면
     * "느리다"는 안내만 한 번 보여준다. 실제 성공/실패 결과는 작업이 실제로 끝났을 때만 반환된다.
     */
    private suspend fun <T> runWithSlowNotice(block: suspend () -> Result<T>): Result<T> = coroutineScope {
        val noticeJob = launch {
            delay(NETWORK_SLOW_MESSAGE_DELAY)
            sendToast(NETWORK_SLOW_MESSAGE)
        }
        val result = block()
        noticeJob.cancel()
        result
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityDetailEvent.ShowToast(message)) }
    }

    companion object {
        private val NETWORK_SLOW_MESSAGE_DELAY = 2.seconds
        private const val NETWORK_SLOW_MESSAGE = "네트워크가 불안정합니다. 연결되면 자동으로 처리됩니다."
    }

}
