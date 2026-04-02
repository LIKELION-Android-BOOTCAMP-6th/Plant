package com.a32b.plant.ui.feature.community.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.ActivityType
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.PostAuthor
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.repository.PostRepository
import com.a32b.plant.data.repository.PotRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommunityPostUiState(
    val postId: String? = null,
    val title: String = "",
    val content: String? = null,
    val selected: List<String> = emptyList(),
    val potId: String? = null,
    val studyLogs: List<StudyLog>? = null,
    val isDismissDialogShow: Boolean = false,
    val isShared: Boolean = false,
    val tags: List<String> = emptyList()
)
sealed class CommunityPostEvent{
    data class NavigateToDetail(val postId: String) : CommunityPostEvent()
}
class CommunityPostViewModel(private val repository: PostRepository, private val potRepository: PotRepository,
                             private var postId: String?, private val potId: String?,  private val title: String?, private val studyLogIds: List<String>?
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityPostUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityPostEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    init {
        fetchTags()
    }

    private fun fetchTags(){
        viewModelScope.launch {
            potRepository.getAvailableTags().collectLatest { tags ->
                getTags(tags)
            }
        }
    }
    // ✅ 기존 글을 불러오는 함수
    fun getPost(postId: String) {
        viewModelScope.launch {
            repository.getPostDetail(postId).firstOrNull()?.let { post ->
                Log.d("getPost", post.tag.toString())
                if (post.tag.contains("공유"))
                    _uiState.update { it.copy(isShared = true,postId = post.postId,title = post.title, studyLogs = post.studyLogs, selected = post.tag) }
                else
                    _uiState.update { it.copy(postId = post.postId,title = post.title, content = post.content, selected = post.tag) }
            }
        }
    }

    fun getStudyLog(){
        //개별 학습 기록 공유 시
        viewModelScope.launch {
            val logs = studyLogIds!!.mapNotNull { id->
                potRepository.getSelectedStudyLog(potId!!, id)
            }
            _uiState.update { it.copy(studyLogs = (it.studyLogs?:emptyList()) + logs) }
        }
    }
    fun getTags(list: List<String>) = _uiState.update { it.copy(tags = list) }
    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }

    fun onSelectedTagChange(tag:List<String>) = _uiState.update { it.copy(selected = tag) }
    fun onIsDismissDialogShowChange() = _uiState.update { it.copy(isDismissDialogShow = !it.isDismissDialogShow) }

    fun onIsSharedChange(){
        potId?.let {
            _uiState.update { it.copy(isShared = true) }
            getStudyLog()
            onTitleChange(title!!)
        }
    }

    fun savePost(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isShared = _uiState.value.isShared

            try {
                //게시글 수정
                if (postId != null) {
                    //게시글이 공유글인지 판별 후
                    repository.updatePost(
                        isShared = isShared,
                        postId = postId!!,
                        title = _uiState.value.title,
                        content = if(isShared) null else _uiState.value.content,
                        tag = if (isShared) null else _uiState.value.selected,
                        createdAt = if(isShared) null else Timestamp.now()
                    )


                } else {
//                     ✅ 새 글 작성 모드
                    val newPost = Post(
                        author = PostAuthor(
                            CurrentUser.uid,
                            CurrentUser.nickname,
                            CurrentUser.profileImg),
                        title = _uiState.value.title,
                        content = if(isShared) null else _uiState.value.content,
                        studyLogs = if(isShared)_uiState.value.studyLogs else null,
                        tag = _uiState.value.selected
                    )
                    postId = repository.savePost(newPost, CommunityActivity(type = ActivityType.POST, title = _uiState.value.title))
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
            _eventChannel.send(CommunityPostEvent.NavigateToDetail(postId!!))
        }
    }
}