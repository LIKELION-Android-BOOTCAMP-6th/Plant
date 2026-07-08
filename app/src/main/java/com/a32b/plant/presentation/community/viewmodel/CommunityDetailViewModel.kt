package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.SavedStateHandle
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.origin.OldPostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityDetailUiState(
    val comment: String = "",
    val commentList: List<Comment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isShared: Boolean = false,
    val studyLogs: List<StudyLog>? = emptyList(),

    // 댓글 수정 상태
    val editingCommentId: String? = null,
    val editingCommentText: String = "",

    // 댓글 삭제 대상 ID
    val deletingCommentId: String? = null
)
@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    private val repository: OldPostRepository,
    private val addCommentUseCase: AddCommentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDetailUiState())
    val uiState = _uiState.asStateFlow()

    val postId: String = savedStateHandle.get<String>("postId") ?: ""
    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()


    private val _isLikeProcessing = MutableStateFlow(false)
    val isLikeProcessing: StateFlow<Boolean> = _isLikeProcessing.asStateFlow()

    init {
        loadPostDetail()
        loadComment()
        onIsSharedChange()
    }

    private fun loadPostDetail() {
        repository.getPostDetail(postId).onEach { _post.value = it }.launchIn(viewModelScope)

    }

    //공유 여부 체크
    fun onIsSharedChange(){
        if (!_post.value?.studyLogs.isNullOrEmpty())
            _uiState.update { it.copy(isShared = true) }
    }

    private fun loadComment(){
        viewModelScope.launch {
            _uiState.update { it.copy(commentList = repository.getComments(postId)) }
        }
    }

    fun onCommentChange(newText: String) { _uiState.update { it.copy(comment = newText) } }

    fun openDeleteDialog() { _showDeleteDialog.value = true }
    fun closeDeleteDialog() { _showDeleteDialog.value = false }

    fun addComment() {

        if (_uiState.value.comment.isBlank()) return

        viewModelScope.launch {
            addCommentUseCase(postId, _post.value?.title ?: "", _uiState.value.comment)
                .onSuccess {
                    //TODO 댓글 등록 성공 토스트 띄우기
                    _uiState.update{it.copy(comment = "")}
                    loadComment()
                }
                .onFailure {
                    //TODO 에러 메세지 오류 토스트 띄우기
                }
//            try {
//                repository.addComment(postId = postId,
//                    comment = Comment(
//                        user = CommentUser(CurrentUser.uid, CurrentUser.nickname, CurrentUser.profileImg),
//                        content = _uiState.value.comment),
//                    activity = CommunityActivity(
//                        type = ActivityType.COMMENT,
//                        title = post.value!!.title,
//                        comment = _uiState.value.comment
//                    )
//                )
//            } catch (e: Exception) { e.printStackTrace() }


            // loadComment()를 launch 안으로 이동하여 addComment 완료 '후' 호출되도록 보장

        }
//        _uiState.update{it.copy(comment = "")}
//        loadComment()
    }

    fun deletePost(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
                onComplete()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun toggleLike() {
        val currentPost = _post.value ?: return

        if (currentPost.author.id == CurrentUser.uid) return

        if (_isLikeProcessing.value) return

        viewModelScope.launch {
            _isLikeProcessing.value = true
            try {
                repository.toggleLike(postId, CurrentUser.uid, currentPost.isLiked, currentPost.title)
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isLikeProcessing.value = false
            }
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
            try {
                repository.updateComment(postId, commentId, state.editingCommentText)
            } catch (e: Exception) { e.printStackTrace() }
            _uiState.update { it.copy(editingCommentId = null, editingCommentText = "") }
            loadComment()
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
            try {
                repository.deleteComment(postId, commentId)
            } catch (e: Exception) { e.printStackTrace() }
            _uiState.update { it.copy(deletingCommentId = null) }
            loadComment()
        }
    }

}