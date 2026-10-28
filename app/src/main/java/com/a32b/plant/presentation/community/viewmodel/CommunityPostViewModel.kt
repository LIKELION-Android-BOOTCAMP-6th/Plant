package com.a32b.plant.presentation.community.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.withSlowNotice
import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.CreatePostUseCase
import com.a32b.plant.domain.service.PostCooldownManager
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
    val extraContent: String? = null,
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
    object NavigateBack : CommunityPostEvent()
}
@HiltViewModel
class CommunityPostViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val createPostUseCase: CreatePostUseCase,
    private val postCooldownManager: PostCooldownManager,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val getSelectedStudyLogUseCase: GetSelectedStudyLogUseCase,
    private val postUpdateNotifier: PostUpdateNotifier,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CommunityPostUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityPostEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private var postId: String? = savedStateHandle["postId"]
    private val potId: String? = savedStateHandle["potId"]
    private val title: String? = savedStateHandle["title"]
    private val studyLogIds: List<String>? = savedStateHandle.get<Array<String>>("studyLogIds")?.toList()
    private val tagId: String? = savedStateHandle["tagId"]

    init {
        fetchTags()
        // 공유 모드(potId 존재): 학습 기록 로드 후 getStudyLog()에서 loaded()
        onIsSharedChange()
        when {
            // 수정 모드: 기존 게시글 로드 후 getPost()에서 loaded()
            postId != null -> getPost(postId!!)
            // 일반 신규 작성: 불러올 데이터가 없으므로 즉시 loaded()
            potId == null -> loaded()
        }
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
                    _uiState.update { it.copy(
                        tags = tags,
                        isTagSheetShown = postId == null && potId == null
                    ) }
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
    private fun getPost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 수정할 대상을 못 불러오면 빈 폼으로 기존 글을 덮어쓸 수 있으므로 이전 화면으로 복귀
            // 토스트가 화면 이탈 전에 처리되도록 sendToast(launch) 대신 순서대로 직접 send
            repository.getPostDetail(postId)
                .onSuccess { post ->
                    if (post == null) {
                        _eventChannel.send(CommunityPostEvent.ShowToast("삭제된 게시글입니다."))
                        _eventChannel.send(CommunityPostEvent.NavigateBack)
                        return@onSuccess
                    }
                    if (post.isShared?:false){
                        _uiState.update { it.copy(isShared = true, postId = post.postId, title = post.title, studyLogs = post.studyLogs, selected = post.tag, extraContent = post.content) }
                    }
                    else{
                        _uiState.update { it.copy( postId = post.postId,title = post.title, content = post.content, selected = post.tag) }
                    }
                }
                .onFailure { e ->
                    // 실패 로그는 Repository에서 이미 기록
                    _eventChannel.send(CommunityPostEvent.ShowToast(e.message))
                    _eventChannel.send(CommunityPostEvent.NavigateBack)
                }
            loaded() // BaseViewModel: 성공/실패 모두 조회 시도 후 LoadableScreen 해제
        }
    }

    // 모든 종료 경로에서 loaded() 호출 — 빠지면 공유 모드 진입 시 무한 로딩
    private fun getStudyLog() {
        val currentPotId = potId ?: return loaded()
        val currentStudyLogIds = studyLogIds ?: return loaded()
        //개별 학습 기록 공유 시
        viewModelScope.launch(Dispatchers.IO) {
            val user = when (val result = ensureCurrentUserUseCase()) {
                is Result.Success -> result.data
                is Result.Failure -> {
                    loaded()
                    return@launch // 세션 만료는 UseCase 내부에서 이미 알림
                }
            }
            val logs = currentStudyLogIds.mapNotNull { id ->
                getSelectedStudyLogUseCase(user.uid, currentPotId, id)
            }
            _uiState.update { it.copy(studyLogs = logs) }
            loaded() // BaseViewModel: 학습 기록 로드 후 LoadableScreen 해제
        }
    }
    fun getTags(list: List<Tag>) = _uiState.update { it.copy(tags = list) }
    fun onTitleChange(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChange(content: String) = _uiState.update { it.copy(content = content) }
    fun onExtraContentChange(content: String) = _uiState.update { it.copy(extraContent = content) }

    fun onSelectedTagChange(tag:Tag) {
        if (!_uiState.value.isShared) {
            _uiState.update { it.copy(selected = tag, isTagSheetShown = false) }
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

    fun savePost() {
        if (_uiState.value.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }

            val isShared = _uiState.value.isShared
            val selectedTag = _uiState.value.selected ?: run {
                _uiState.update { it.copy(isSubmitting = false) }
                return@launch
            }

            if (postId != null) {
                withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                    repository.updatePost(
                        isShared = isShared,
                        postId = postId!!,
                        title = _uiState.value.title,
                        content = if (isShared) _uiState.value.extraContent else _uiState.value.content,
                        tag = if (isShared) null else selectedTag
                    )
                }.onSuccess {
                    // 목록 화면 복귀 시 서버 재조회 없이 변경된 제목/내용/태그가 즉시 반영되도록 알림
                    postUpdateNotifier.notifyPostUpdated(
                        PostUpdate(
                            postId = postId!!,
                            title = _uiState.value.title,
                            content = if (isShared) _uiState.value.extraContent else _uiState.value.content,
                            tag = if (isShared) null else selectedTag,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    _eventChannel.send(CommunityPostEvent.ShowToast("수정되었습니다!"))
                    _eventChannel.send(CommunityPostEvent.NavigateToDetail(postId!!))
                }.onFailure { e ->
                    Log.e("CommunityPostVM", "게시글 수정 실패: ${e.message}")
                    _eventChannel.send(CommunityPostEvent.ShowToast("수정에 실패했습니다."))
                }
            } else {
                if (!postCooldownManager.canPost()) {
                    val remaining = postCooldownManager.getRemainingSeconds()
                    sendToast("게시글 작성은 ${remaining}초 후에 가능합니다")
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }

                withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                    createPostUseCase(
                        isShared = isShared,
                        title = _uiState.value.title,
                        content = if (isShared) _uiState.value.extraContent else _uiState.value.content,
                        studyLogs = _uiState.value.studyLogs,
                        tag = selectedTag
                    )
                }.onSuccess { newPostId ->
                    postCooldownManager.recordPost()
                    postId = newPostId
                    _eventChannel.send(CommunityPostEvent.ShowToast("성공적으로 등록되었습니다!"))
                    _eventChannel.send(CommunityPostEvent.NavigateToDetail(newPostId))
                }.onFailure { e ->
                    Log.e("CommunityPostVM", "게시글 등록 실패: ${e.message}")
                    _eventChannel.send(CommunityPostEvent.ShowToast("등록에 실패했습니다."))
                }
            }

            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityPostEvent.ShowToast(message)) }
    }
}
