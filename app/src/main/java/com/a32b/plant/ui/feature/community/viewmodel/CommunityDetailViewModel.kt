package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.Author
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CommunityDetailViewModel(
    private val repository: PostRepository,
    private val postId: String
) : ViewModel() {

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    val currentUser: StateFlow<Author?> = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var commentText = MutableStateFlow("")

    init {
        loadPostDetail()
    }

    private fun loadPostDetail() {
        repository.getPost(postId)
            .onEach { _post.value = it }
            .launchIn(viewModelScope)
    }

    fun onCommentChange(newText: String) {
        commentText.value = newText
    }

    fun addComment() {
        val user = currentUser.value
        val content = commentText.value

        if (user == null || content.isBlank()) return

        viewModelScope.launch {
            try {

                repository.addComment(
                    postId = postId,
                    nickName = user.nickname,
                    content = content
                )
                commentText.value = ""
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deletePost(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePost(postId)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}