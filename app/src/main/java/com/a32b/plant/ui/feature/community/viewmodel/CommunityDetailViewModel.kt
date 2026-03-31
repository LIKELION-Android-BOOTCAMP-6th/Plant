package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.a32b.plant.core.util.ActivityType
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Comment
import com.a32b.plant.data.model.CommentUser
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CommunityDetailUiState(
    //⭐⭐⭐⭐다른 것들도 여기로 넣어서 관리하기
    val comment: String = "",
    val commentList: List<Comment> = emptyList()

)
class CommunityDetailViewModel(
    private val repository: PostRepository,
    private val postId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()


    private val _isLikeProcessing = MutableStateFlow(false)
    val isLikeProcessing: StateFlow<Boolean> = _isLikeProcessing.asStateFlow()

    init {
        loadPostDetail()
        loadComment()
    }

    private fun loadPostDetail() {
        repository.getPostDetail(postId).onEach { _post.value = it }.launchIn(viewModelScope)
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
            try {
                repository.addComment(postId = postId,
                    comment = Comment(
                        user = CommentUser(CurrentUser.uid, CurrentUser.nickname, CurrentUser.profileImg),
                        content = _uiState.value.comment),
                    activity = CommunityActivity(
                        type = ActivityType.COMMENT,
                        title = post.value!!.title,
                        comment = _uiState.value.comment
                    )
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
        _uiState.update{it.copy(comment = "")}
        loadComment()
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
                repository.toggleLike(postId, CurrentUser.uid, currentPost.isLiked)
            } catch (e: Exception) { 
                e.printStackTrace() 
            } finally {
                _isLikeProcessing.value = false
            }
        }
    }
}