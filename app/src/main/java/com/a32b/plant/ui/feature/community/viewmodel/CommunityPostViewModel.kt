package com.a32b.plant.ui.feature.community.viewmodel

import android.R.attr.author
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.ui.feature.community.ui.Post
import com.a32b.plant.data.repository.PostRepository
import kotlinx.coroutines.launch

class CommunityPostViewModel(private val repository: PostRepository, private val type: String?) : ViewModel() {

    // ✅ 화면에서 등록 버튼을 누르면 실행될 함수
    fun savePost(title: String, content: String, tag: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 🚀 Post의 변수 이름들과 100% 일치해야 에러가 안 납니다!
                // ViewModel 내부의 savePost 함수 부분
                val newPost = Post(
                    id = "",
                   // nickName = " ",
                    content = "[$tag] $title\n$content",
                    commentCount = 0,
                    likeCount = 0,
                    isLiked = false,
                    createdAt = "2026-03-25"
                )

                repository.uploadPost(newPost)
                onComplete(true) // 성공 알림
            } catch (e: Exception) {
                onComplete(false) // 실패 알림
            }
        }
    }
}