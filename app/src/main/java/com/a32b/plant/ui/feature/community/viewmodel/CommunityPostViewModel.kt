package com.a32b.plant.ui.feature.community.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.PostAuthor
import com.a32b.plant.data.repository.PostRepository
import com.a32b.plant.data.repository.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CommunityPostUiState(
    val postId: String? = null,
    val title: String = "",
    val content: String = "",
    val selected: List<String> = emptyList(),
    val potId: String? = null,
    val studyLogs: List<String>? = null
)
class CommunityPostViewModel(private val repository: PostRepository, private val potRepository: PotRepository,
                             private val postId: String?, private val potId: String?, private val studyLogs: List<String>?) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityPostUiState())
    val uiState = _uiState.asStateFlow()


    // ✅ 기존 글을 불러오는 함수
    fun getPost(postId: String) {
        viewModelScope.launch {
            repository.getPost(postId).firstOrNull()?.let { post ->
                _uiState.update { it.copy(postId = post.postId,title = post.title, content = post.content, selected = post.tag) }
            }
        }
    }

    fun getStudyLog(potId: String){
        //개별 학습 기록 공유 시
    }
    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }
    fun onSelectedTagChange(tag:List<String>) = _uiState.update { it.copy(selected = tag) }

    fun savePost(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                if (postId != null) {
                    // ✅ 수정 모드
                    repository.updatePost(postId, _uiState.value.title, _uiState.value.content, _uiState.value.selected)
                } else {
//                     ✅ 새 글 작성 모드
                    val newPost = Post(
                        author = PostAuthor(
                            CurrentUser.uid,
                            CurrentUser.nickname,
                            CurrentUser.profileImg
                        ),
                        title = _uiState.value.title,
                        content = _uiState.value.content,
                        tag = _uiState.value.selected
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