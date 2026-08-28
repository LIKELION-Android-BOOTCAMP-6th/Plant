package com.a32b.plant.presentation.community.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.withSlowNotice
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.domain.usecase.community.ToggleLikeUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class CommunityDetailUiState(
    val comment: String = "",
    val commentList: List<Comment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isShared: Boolean = false,
    val studyLogs: List<StudyLog>? = emptyList(),
    val currentUid: String = "",
    val currentNickname: String = "",
    val isCommentSubmitting: Boolean = false,
    val isRefreshing: Boolean = false,

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
    private val repository: CommunityRepository,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val LIKE_DEBOUNCE = 500.milliseconds
    }

    private val _uiState = MutableStateFlow(CommunityDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityDetailEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    val postId: String = savedStateHandle.get<String>("postId") ?: ""
    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    // 좋아요 디바운스 상태
    private var likeDebounceJob: Job? = null

    init {
        ensureCurrentUserUseCase { user -> _uiState.update { it.copy(currentUid = user.uid, currentNickname = user.nickname) } }
        refresh()
    }

    //공유 여부 체크
    fun onIsSharedChange(){
        if (!_post.value?.studyLogs.isNullOrEmpty())
            _uiState.update { it.copy(isShared = true) }
    }

    /** 게시글 상세 + 댓글 목록을 단건 조회로 다시 불러온다 (최초 진입 + pull-to-refresh 공용) */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val postDeferred = async { repository.getPostDetail(postId) }
            val commentsDeferred = async { repository.getComments(postId) }

            postDeferred.await()
                .onSuccess { post ->
                    _post.value = post
                    onIsSharedChange()
                }
                .onFailure { e -> sendToast(e.message) }

            commentsDeferred.await()
                .onSuccess { comments -> _uiState.update { it.copy(commentList = comments) } }
                .onFailure { e -> sendToast(e.message) }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onCommentChange(newText: String) { _uiState.update { it.copy(comment = newText) } }

    fun openDeleteDialog() { _showDeleteDialog.value = true }
    fun closeDeleteDialog() { _showDeleteDialog.value = false }

    fun addComment() {

        if (_uiState.value.comment.isBlank() || _uiState.value.isCommentSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSubmitting = true) }

            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                addCommentUseCase(postId, _post.value?.title ?: "", _uiState.value.comment)
            }
                .onSuccess {
                    _uiState.update { it.copy(comment = "") }
                    refresh()
                }
                .onFailure { e ->
                    // UnknownUser는 AddCommentUseCase 내부에서 이미 세션 만료 이벤트를 발송함
                    sendToast(e.message)
                }

            _uiState.update { it.copy(isCommentSubmitting = false) }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                repository.deletePost(postId, _post.value?.activityId ?: "")
            }
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

        // 낙관적 UI: 로컬 즉시 토글
        val newIsLiked = !currentPost.isLiked
        val delta = if (newIsLiked) 1 else -1
        _post.value = currentPost.copy(
            isLiked = newIsLiked,
            likeCount = (currentPost.likeCount + delta).coerceAtLeast(0)
        )

        // 디바운스: 이전 타이머 취소하고 새로 시작
        likeDebounceJob?.cancel()
        likeDebounceJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE)
            commitLike()
        }
    }

    private suspend fun commitLike() {
        val post = _post.value ?: return
        toggleLikeUseCase(postId, post.author.id, post.title)
            .onSuccess { wasLiked ->
                // 서버가 실제로 반영한 최종 상태 (wasLiked=true이면 취소됐으니 최종은 false)
                val serverFinal = !wasLiked
                if (_post.value?.isLiked != serverFinal) {
                    // 다른 기기에서 상태가 바뀐 케이스 → 서버 기준으로 UI 정정
                    _post.value = _post.value?.copy(isLiked = serverFinal)
                }
            }
            .onFailure { e ->
                sendToast(e.message)
                // 정확한 롤백값을 모르므로 서버 상태 재조회로 정합성 확보
                refresh()
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
            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                repository.updateComment(postId, commentId, state.editingCommentText)
            }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            _uiState.update { it.copy(editingCommentId = null, editingCommentText = "") }
            refresh()
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
            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                repository.deleteComment(postId, commentId)
            }
                .onFailure { e ->
                    if (e is AppError.UnknownUser) ensureCurrentUserUseCase()
                    sendToast(e.message)
                }
            _uiState.update { it.copy(deletingCommentId = null) }
            refresh()
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityDetailEvent.ShowToast(message)) }
    }

}
