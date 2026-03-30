package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityPostViewModel(private val repository: PostRepository) : ViewModel() {

    // ✅ 기존 글을 불러오는 함수
    fun getPost(postId: String, onLoaded: (Post) -> Unit) {
        viewModelScope.launch {
            repository.getPost(postId).firstOrNull()?.let { onLoaded(it) }
        }
    }

    fun savePost(postId: String?, title: String, content: String, tag: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (postId != null) {
                    // ✅ 수정 모드
                    repository.updatePost(postId, title, content, tag)
                } else {
                    // ✅ 새 글 작성 모드
                    val newPost = Post(
                        id = "",
                        title = title,
                        nickName = "성호",
                        content = content,
                        tag = tag,
                        commentCount = 0,
                        likeCount = 0,
                        createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    repository.uploadPost(newPost)
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }
}