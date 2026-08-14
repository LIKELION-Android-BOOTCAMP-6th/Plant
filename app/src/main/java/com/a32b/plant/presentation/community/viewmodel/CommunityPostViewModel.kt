package com.a32b.plant.presentation.community.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.withSlowNotice
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.CreatePostUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studyLog.GetSelectedStudyLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject

data class CommunityPostUiState(
    val postId: String? = null,
    val title: String = "",
    val content: String? = null,
    val selected: Tag? = null,
    val potId: String? = null,
    val studyLogs: List<StudyLog>? = null,
    val isDismissDialogShow: Boolean = false,
    val isShared: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val isTagSheetShown: Boolean = false,
    val isSubmitting: Boolean = false
)
sealed class CommunityPostEvent{
    data class NavigateToDetail(val postId: String) : CommunityPostEvent()
    data class ShowToast(val message: String) : CommunityPostEvent()
}
@HiltViewModel
class CommunityPostViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val createPostUseCase: CreatePostUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val getSelectedStudyLogUseCase: GetSelectedStudyLogUseCase,
    savedStateHandle: SavedStateHandle,

    ) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityPostUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityPostEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private var postId: String? = savedStateHandle["postId"]
    private val potId: String? = savedStateHandle["potId"]
    private val title: String? = savedStateHandle["title"]
    private val studyLogIds: List<String>? = savedStateHandle["studyLogIds"]
    private val tagId: String? = savedStateHandle["tagId"]

    init {
        fetchTags()
        onIsSharedChange()
    }
    fun onIsTagSheetShownChange() {
        if (!_uiState.value.isShared) {
            _uiState.update { it.copy(isTagSheetShown = !_uiState.value.isTagSheetShown) }
        }
    }
    private fun fetchTags(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.getTags()
                .onSuccess { tags ->
                    _uiState.update { it.copy(tags = tags) }
                    matchSelectedTag()
                }
                .onFailure { e -> Log.e("CommunityPostVM", "태그 목록 조회 실패: ${e.message}") }
        }
    }
    fun matchSelectedTag(){
        val idToMatch = tagId ?: return // tagId가 없으면 중단

        //!! 제거: find 결과를 안전하게 처리
        val foundTag = _uiState.value.tags.find { it.id == idToMatch }
        foundTag?.let {
            _uiState.update { state -> state.copy(selected = it)}
        } ?: run {
            Log.e("CommunityPostVM", "전달된 tagId($idToMatch)를 tags 리스트에서 찾을 수 없습니다.")
        }
    }
    // ✅ 기존 글을 불러오는 함수
    fun getPost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getPostDetail(postId)
                .onSuccess { post ->
                    post ?: return@onSuccess
                    if (post.isShared?:false){
                        _uiState.update { it.copy(isShared = true,postId = post.postId,title = post.title, studyLogs = post.studyLogs, selected = post.tag) }

                    }
                    else{
                        _uiState.update { it.copy( postId = post.postId,title = post.title, content = post.content, selected = post.tag) }
                    }
                }
                .onFailure { e -> Log.e("CommunityPostVM", "게시글 조회 실패: ${e.message}") }
        }
    }

    fun getStudyLog() {
        val currentPotId = potId ?: return
        val currentStudyLogIds = studyLogIds ?: return
        //개별 학습 기록 공유 시
        viewModelScope.launch(Dispatchers.IO) {
            val user = when (val result = ensureCurrentUserUseCase()) {
                is Result.Success -> result.data
                is Result.Failure -> return@launch // 세션 만료는 UseCase 내부에서 이미 알림
            }
            val logs = currentStudyLogIds.mapNotNull { id ->
                getSelectedStudyLogUseCase(user.uid, currentPotId, id)
            }
            _uiState.update { it.copy(studyLogs = (it.studyLogs ?: emptyList()) + logs) }
        }
    }
    fun getTags(list: List<Tag>) = _uiState.update { it.copy(tags = list) }
    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }

    fun onSelectedTagChange(tag:Tag) {
        if (!_uiState.value.isShared) {
            _uiState.update { it.copy(selected = tag) }
        }
    }
    fun onIsDismissDialogShowChange() = _uiState.update { it.copy(isDismissDialogShow = !it.isDismissDialogShow) }

    fun onIsSharedChange(){
        potId?.let {
            _uiState.update { it.copy(isShared = true) }
            getStudyLog()
            title?.let { onTitleChange(it) }
        }
    }

    fun savePost(onComplete: (Boolean) -> Unit) {
        // 등록/수정 중 중복 탭으로 게시글이 여러 번 생성되는 것을 방지
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val isShared = _uiState.value.isShared
            val selectedTag = _uiState.value.selected ?: run {
                _uiState.update { it.copy(isSubmitting = false) }
                return@launch
            }

            if (postId != null) {
                //게시글 수정
                withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                    repository.updatePost(
                        isShared = isShared,
                        postId = postId!!,
                        title = _uiState.value.title,
                        content = if (isShared) null else _uiState.value.content,
                        tag = if (isShared) null else selectedTag
                    )
                }.onSuccess {
                    onComplete(true)
                    _eventChannel.send(CommunityPostEvent.NavigateToDetail(postId!!))
                }.onFailure { e ->
                    Log.e("CommunityPostVM", "게시글 수정 실패: ${e.message}")
                    onComplete(false)
                }
            } else {
                // 새 글 작성 모드
                withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                    createPostUseCase(
                        isShared = isShared,
                        title = _uiState.value.title,
                        content = _uiState.value.content,
                        studyLogs = _uiState.value.studyLogs,
                        tag = selectedTag
                    )
                }.onSuccess { newPostId ->
                    postId = newPostId
                    onComplete(true)
                    _eventChannel.send(CommunityPostEvent.NavigateToDetail(newPostId))
                }.onFailure { e ->
                    Log.e("CommunityPostVM", "게시글 등록 실패: ${e.message}")
                    onComplete(false)
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityPostEvent.ShowToast(message)) }
    }
}
