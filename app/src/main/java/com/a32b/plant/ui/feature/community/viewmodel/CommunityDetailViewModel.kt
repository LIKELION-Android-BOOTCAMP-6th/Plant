package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityDetailViewModel(
    private val repository: PostRepository,
    private val postId: String
) : ViewModel() {

    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()


    var commentText = MutableStateFlow("")


    private val _isLikeProcessing = MutableStateFlow(false)
    val isLikeProcessing: StateFlow<Boolean> = _isLikeProcessing.asStateFlow()

    init { loadPostDetail() }

    private fun loadPostDetail() {
        repository.getPost(postId).onEach { _post.value = it }.launchIn(viewModelScope)
    }

    fun onCommentChange(newText: String) { commentText.value = newText }

    fun openDeleteDialog() { _showDeleteDialog.value = true }
    fun closeDeleteDialog() { _showDeleteDialog.value = false }

    fun addComment() {
        val content = commentText.value

        if (CurrentUser.uid == null || content.isBlank()) return

        viewModelScope.launch {
            try {
                repository.addComment(
                    postId = postId,
                    uid = CurrentUser.uid,
                    nickName = CurrentUser.nickname,
                    content = content
                )
                commentText.value = ""
            } catch (e: Exception) { e.printStackTrace() }
        }
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