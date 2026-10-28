package com.a32b.plant.presentation.community.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.withSlowNotice
import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ReportReason
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.domain.usecase.community.CreateCommunityReportUseCase
import com.a32b.plant.domain.usecase.community.ToggleBookmarkUseCase
import com.a32b.plant.domain.usecase.community.ToggleLikeUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class CommunityDetailUiState(
    val comment: String = "",
    val commentList: List<Comment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isShared: Boolean = false,
    val studyLogs: List<StudyLog>? = emptyList(),
    val currentUid: String = "",
    val currentNickname: String = "",
    val currentProfileImg: String = "",
    val isCommentSubmitting: Boolean = false,
    val isRefreshing: Boolean = false,

    // 댓글 수정 상태
    val editingCommentId: String? = null,
    val editingCommentText: String = "",
    // 저장 요청이 진행 중인 commentId 모음 — 여러 댓글을 번갈아 수정해도 각자 독립적으로 추적
    val editingSubmittingCommentIds: Set<String> = emptySet(),

    // 댓글 삭제 대상 ID
    val deletingCommentId: String? = null,

    val isInitialLoading: Boolean = true,
    val isPostNotFound: Boolean = false,

    // 신고 BottomSheet
    val showReportSheet: Boolean = false,
    val reportTargetType: String = "",
    val reportTargetId: String = "",
    val selectedReportReason: ReportReason? = null,
    val reportDetailText: String = "",
    val isReportSubmitting: Boolean = false
)

sealed class CommunityDetailEvent {
    data class ShowToast(val message: String) : CommunityDetailEvent()
    object NavigateBack : CommunityDetailEvent()
}

@HiltViewModel
class CommunityDetailViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val toggleBookmarkUseCase: ToggleBookmarkUseCase,
    private val createCommunityReportUseCase: CreateCommunityReportUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val postUpdateNotifier: PostUpdateNotifier,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val LIKE_DEBOUNCE = 500.milliseconds
        private val BOOKMARK_DEBOUNCE = 500.milliseconds
        private val BLANK_COMMENT_TOAST_DEBOUNCE = 1000.milliseconds
        private const val BLANK_COMMENT_MESSAGE = "댓글 내용을 입력해주세요"
        private const val COMMENT_NOT_FOUND_MESSAGE = "이미 삭제된 댓글입니다"
    }

    private val _uiState = MutableStateFlow(CommunityDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<CommunityDetailEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    val postId: String = savedStateHandle.get<String>("postId") ?: ""
    private val _showDeleteDialog = mutableStateOf(false)
    val showDeleteDialog: State<Boolean> = _showDeleteDialog

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    // 좋아요/북마크 디바운스 상태
    private var likeDebounceJob: Job? = null
    private var bookmarkDebounceJob: Job? = null
    // delay 구간에서만 true — 재진입 차단 + commitXXX() 시작 직전 false로 해제
    private var likePendingCommit = false
    private var bookmarkPendingCommit = false
    // commitXXX()가 실제 서버 호출 중인 구간에서만 true — refresh()가 아직 커밋되지 않은 값을 서버의 이전 값으로 덮어쓰는 것을 막기 위함
    // (likePendingCommit은 디바운스 호출 직전 false가 되므로, 그 이후 실제 네트워크 왕복 구간은 이 플래그로 별도 보호)
    private var likeCommitInFlight = false
    private var bookmarkCommitInFlight = false

    // 삭제 요청이 진행 중인 댓글 ID — 확인 다이얼로그는 응답 후에야 닫히므로 연타로 인한 중복 삭제/알림을 막기 위함
    // (다이얼로그를 닫고 다른 댓글을 삭제하는 것은 막지 않도록 댓글별로 관리)
    private val deletingInFlightCommentIds = mutableSetOf<String>()

    // blank 상태에서 연타 시 Toast가 줄줄이 쌓이는 것을 막기 위한 디바운스
    private var commentBlankToastJob: Job? = null
    private var editBlankToastJob: Job? = null

    init {
        ensureCurrentUserUseCase { user -> _uiState.update { it.copy(currentUid = user.uid, currentNickname = user.nickname, currentProfileImg = user.profileImg) } }
        loadInitial()
    }

    /** 첫 진입 시 데이터 로딩 — isRefreshing을 건드리지 않아 pull-to-refresh 인디케이터가 나타나지 않음 */
    private fun loadInitial() {
        viewModelScope.launch {
            val postDeferred = async { repository.getPostDetail(postId) }
            val commentsDeferred = async { repository.getComments(postId) }

            postDeferred.await()
                .onSuccess { post ->
                    _post.value = post
                    onIsSharedChange()
                    if (post == null) {
                        _uiState.update { it.copy(isPostNotFound = true) }
                    }
                }
                .onFailure { e -> sendToast(e.message) }

            commentsDeferred.await()
                .onSuccess { comments -> _uiState.update { it.copy(commentList = comments) } }
                .onFailure { e -> sendToast(e.message) }

            _uiState.update { it.copy(isInitialLoading = false) }
        }
    }

    //공유 여부 체크
    fun onIsSharedChange(){
        if (!_post.value?.studyLogs.isNullOrEmpty())
            _uiState.update { it.copy(isShared = true) }
    }

    /** pull-to-refresh 전용: 인디케이터를 표시하며 데이터를 다시 불러온다 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val postDeferred = async { repository.getPostDetail(postId) }
            val commentsDeferred = async { repository.getComments(postId) }

            postDeferred.await()
                .onSuccess { fetchedPost ->
                    if (fetchedPost == null) {
                        _post.value = null
                        onIsSharedChange()
                        _uiState.update { it.copy(isPostNotFound = true) }
                    } else {
                        // 좋아요/북마크가 디바운스 대기 중이거나 서버 커밋이 진행 중이면, 아직 반영되지 않은
                        // 서버의 이전 값으로 덮어쓰지 않고 로컬에서 들고 있던 값을 그대로 유지한다.
                        val current = _post.value
                        val merged = fetchedPost.copy(
                            isLiked = if (isLikeUncommitted()) current?.isLiked ?: fetchedPost.isLiked else fetchedPost.isLiked,
                            likeCount = if (isLikeUncommitted()) current?.likeCount ?: fetchedPost.likeCount else fetchedPost.likeCount,
                            isBookmarked = if (isBookmarkUncommitted()) current?.isBookmarked ?: fetchedPost.isBookmarked else fetchedPost.isBookmarked,
                            bookmarkCount = if (isBookmarkUncommitted()) current?.bookmarkCount ?: fetchedPost.bookmarkCount else fetchedPost.bookmarkCount
                        )
                        _post.value = merged
                        onIsSharedChange()
                        // 댓글 작성/삭제/수정, 신고, 좋아요·북마크 보정 등 상호작용 결과를 목록 화면에 즉시 반영
                        notifyPostChanged(merged)
                    }
                }
                .onFailure { e -> sendToast(e.message) }

            commentsDeferred.await()
                .onSuccess { comments -> _uiState.update { it.copy(commentList = comments) } }
                .onFailure { e -> sendToast(e.message) }

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onCommentChange(newText: String) { _uiState.update { it.copy(comment = newText) } }

    fun openDeleteDialog() { _showDeleteDialog.value = true }
    fun closeDeleteDialog() { _showDeleteDialog.value = false }

    // 진행 중인 디바운스 Job이 있으면 Toast를 다시 보내지 않고 그 Job을 유지, 없으면 새로 보내고 새 Job을 반환
    private fun sendBlankCommentToastIfNeeded(job: Job?): Job {
        if (job?.isActive == true) return job
        sendToast(BLANK_COMMENT_MESSAGE)
        return viewModelScope.launch { delay(BLANK_COMMENT_TOAST_DEBOUNCE) }
    }

    fun addComment() {

        if (_uiState.value.isCommentSubmitting) return
        if (_uiState.value.comment.isBlank()) {
            commentBlankToastJob = sendBlankCommentToastIfNeeded(commentBlankToastJob)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCommentSubmitting = true) }

            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                addCommentUseCase(postId, _post.value?.title ?: "", _uiState.value.comment)
            }
                .onSuccess {
                    _uiState.update { it.copy(comment = "") }
                    refresh()
                }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> Unit
                        else -> sendToast(e.message)
                    }
                }

            _uiState.update { it.copy(isCommentSubmitting = false) }
        }
    }

    fun deletePost() {
        viewModelScope.launch {
            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                repository.deletePost(postId, _post.value?.activityId ?: "")
            }
                .onSuccess { _eventChannel.send(CommunityDetailEvent.NavigateBack) }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> ensureCurrentUserUseCase()
                        else -> sendToast(e.message)
                    }
                }
            closeDeleteDialog()
        }
    }

    fun toggleLike() {
        val currentPost = _post.value ?: return
        if (currentPost.author.id == _uiState.value.currentUid) return
        if (likePendingCommit) return

        // 낙관적 UI: 로컬 즉시 토글
        val newIsLiked = !currentPost.isLiked
        val delta = if (newIsLiked) 1 else -1
        val newLikeCount = (currentPost.likeCount + delta).coerceAtLeast(0)
        _post.value = currentPost.copy(isLiked = newIsLiked, likeCount = newLikeCount)
        // 목록 화면 복귀 시 서버 재조회 없이 즉시 반영되도록 낙관적 결과를 바로 알림
        postUpdateNotifier.notifyPostUpdated(
            PostUpdate(postId = postId, isLiked = newIsLiked, likeCount = newLikeCount)
        )

        likePendingCommit = true
        likeDebounceJob = viewModelScope.launch {
            delay(LIKE_DEBOUNCE)
            // onCleared()가 먼저 실행됐다면 flag가 이미 false → 중복 커밋 방지
            if (!likePendingCommit) return@launch
            likePendingCommit = false
            commitLike()
        }
    }

    fun toggleBookmark() {
        val currentPost = _post.value ?: return
        if (bookmarkPendingCommit) return

        val newIsBookmarked = !currentPost.isBookmarked
        val delta = if (newIsBookmarked) 1 else -1
        val newBookmarkCount = (currentPost.bookmarkCount + delta).coerceAtLeast(0)
        _post.value = currentPost.copy(isBookmarked = newIsBookmarked, bookmarkCount = newBookmarkCount)
        postUpdateNotifier.notifyPostUpdated(
            PostUpdate(postId = postId, isBookmarked = newIsBookmarked, bookmarkCount = newBookmarkCount)
        )

        bookmarkPendingCommit = true
        bookmarkDebounceJob = viewModelScope.launch {
            delay(BOOKMARK_DEBOUNCE)
            if (!bookmarkPendingCommit) return@launch
            bookmarkPendingCommit = false
            commitBookmark()
        }
    }

    private suspend fun commitBookmark() {
        val post = _post.value ?: return
        val localIsBookmarked = post.isBookmarked
        bookmarkCommitInFlight = true
        try {
            // NonCancellable: viewModelScope 취소(화면 이탈) 후에도 Firestore 쓰기가 완료되도록 보장 (Case B)
            withContext(NonCancellable) {
                toggleBookmarkUseCase(postId, post.title)
            }.onSuccess { wasBookmarked ->
                    val serverFinal = !wasBookmarked
                    // 커밋 이후 사용자가 또 탭했다면(localIsBookmarked != 현재값) 덮어쓰지 않음
                    // → 다른 기기에서 상태가 바뀐 케이스만 보정
                    if (_post.value?.isBookmarked == localIsBookmarked && _post.value?.isBookmarked != serverFinal) {
                        val delta = if (serverFinal) 1 else -1
                        val correctedCount = ((_post.value?.bookmarkCount ?: 0) + delta).coerceAtLeast(0)
                        _post.value = _post.value?.copy(isBookmarked = serverFinal, bookmarkCount = correctedCount)
                        postUpdateNotifier.notifyPostUpdated(
                            PostUpdate(postId = postId, isBookmarked = serverFinal, bookmarkCount = correctedCount)
                        )
                    }
                }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> {
                            // onCleared 경로(viewModelScope 취소됨): 화면 없으므로 세션 복구 불필요
                            if (viewModelScope.isActive) ensureCurrentUserUseCase()
                        }
                        else -> {
                            // 화면 이탈 후 실행(onCleared 경로)이면 viewModelScope가 취소돼 sendToast/refresh는 no-op
                            sendToast(e.message)
                            refresh()
                        }
                    }
                }
        } finally {
            bookmarkCommitInFlight = false
        }
    }

    private suspend fun commitLike() {
        val post = _post.value ?: return
        val localIsLiked = post.isLiked
        likeCommitInFlight = true
        try {
            // NonCancellable: viewModelScope 취소(화면 이탈) 후에도 Firestore 쓰기가 완료되도록 보장 (Case B)
            withContext(NonCancellable) {
                toggleLikeUseCase(postId, post.author.id, post.title)
            }.onSuccess { wasLiked ->
                    // 서버가 실제로 반영한 최종 상태 (wasLiked=true이면 취소됐으니 최종은 false)
                    val serverFinal = !wasLiked
                    // 커밋 이후 사용자가 또 탭했다면(localIsLiked != 현재값) 덮어쓰지 않음
                    // → 다른 기기에서 상태가 바뀐 케이스만 보정
                    if (_post.value?.isLiked == localIsLiked && _post.value?.isLiked != serverFinal) {
                        val delta = if (serverFinal) 1 else -1
                        val correctedCount = ((_post.value?.likeCount ?: 0) + delta).coerceAtLeast(0)
                        _post.value = _post.value?.copy(isLiked = serverFinal, likeCount = correctedCount)
                        postUpdateNotifier.notifyPostUpdated(
                            PostUpdate(postId = postId, isLiked = serverFinal, likeCount = correctedCount)
                        )
                    }
                }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> {
                            // onCleared 경로(viewModelScope 취소됨): 화면 없으므로 세션 복구 불필요
                            if (viewModelScope.isActive) ensureCurrentUserUseCase()
                        }
                        else -> {
                            // 화면 이탈 후 실행(onCleared 경로)이면 viewModelScope가 취소돼 sendToast/refresh는 no-op
                            sendToast(e.message)
                            // 정확한 롤백값을 모르므로 서버 상태 재조회로 정합성 확보
                            refresh()
                        }
                    }
                }
        } finally {
            likeCommitInFlight = false
        }
    }

    private fun isLikeUncommitted() = likePendingCommit || likeCommitInFlight
    private fun isBookmarkUncommitted() = bookmarkPendingCommit || bookmarkCommitInFlight

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
        if (commentId in state.editingSubmittingCommentIds) return
        if (state.editingCommentText.isBlank()) {
            editBlankToastJob = sendBlankCommentToastIfNeeded(editBlankToastJob)
            return
        }

        // 수정 중 다른 곳에서 삭제돼 목록에서 사라진 댓글 — 저장해도 실패하므로 수정 모드를 닫고 목록을 갱신
        val comment = state.commentList.firstOrNull { it.commentId == commentId }
        if (comment == null) {
            cancelEditComment()
            sendToast(COMMENT_NOT_FOUND_MESSAGE)
            refresh()
            return
        }

        viewModelScope.launch {
            // 저장 중 화면을 이탈해도 반영되려면 updateComment의 batch가 첫 중단점 전에 SDK에 적재돼야 함
            // → 이 코루틴에서 updateComment 호출 앞에 suspend 지점을 추가하지 말 것
            _uiState.update { it.copy(editingSubmittingCommentIds = it.editingSubmittingCommentIds + commentId) }

            // updateComment()가 safeRunCatching으로 감싸져 항상 Result를 반환한다는 전제로,
            // 아래 editingSubmittingCommentIds 제거가 항상 실행됨. 이 함수가 예외를 던지도록 바뀌면
            // commentId가 Set에 영구히 남아 해당 댓글의 저장/취소 버튼이 계속 비활성 상태로 고정됨
            withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                repository.updateComment(postId, commentId, state.editingCommentText, comment.activityId)
            }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> ensureCurrentUserUseCase()
                        else -> sendToast(e.message)
                    }
                }
            _uiState.update {
                // 응답을 받는 동안 다른 댓글 수정으로 전환됐다면 그 상태를 덮어쓰지 않음
                val stillEditingSame = it.editingCommentId == commentId
                it.copy(
                    editingCommentId = if (stillEditingSame) null else it.editingCommentId,
                    editingCommentText = if (stillEditingSame) "" else it.editingCommentText,
                    editingSubmittingCommentIds = it.editingSubmittingCommentIds - commentId
                )
            }
            refresh()
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
        if (!deletingInFlightCommentIds.add(commentId)) return

        viewModelScope.launch {
            try {
                // 댓글·activity 삭제는 repository의 트랜잭션 하나로 처리돼 화면 이탈과 무관하게 완료된다.
                // NonCancellable은 이탈 후에도 삭제 성공 시 목록 화면의 댓글 수 갱신 알림을 보내기 위한 용도.
                withContext(NonCancellable) {
                    withSlowNotice(onSlow = { sendToast(NETWORK_SLOW_MESSAGE) }) {
                        repository.deleteComment(postId, commentId)
                    }
                        // 이미 다른 곳에서 지워진 댓글이면(false) 댓글 수가 이미 반영됐으므로 알리지 않음
                        .onSuccess { deleted -> if (deleted) notifyCommentDeleted() }
                }
                    .onFailure { e ->
                        when (e) {
                            is AppError.UnknownUser -> ensureCurrentUserUseCase()
                            else -> sendToast(e.message)
                        }
                    }
                // 응답을 받는 동안 다른 댓글의 삭제 다이얼로그로 전환됐다면 그 상태를 닫지 않음
                _uiState.update {
                    it.copy(deletingCommentId = if (it.deletingCommentId == commentId) null else it.deletingCommentId)
                }
                refresh()
            } finally {
                deletingInFlightCommentIds.remove(commentId)
            }
        }
    }

    // 이탈 후에는 refresh()가 실행되지 않아 목록이 삭제된 댓글 수를 모르므로, 로컬 값 기준으로 먼저 알림
    // (화면에 머물러 있으면 뒤이은 refresh()가 서버 값으로 다시 알려 보정)
    private fun notifyCommentDeleted() {
        val post = _post.value ?: return
        val updatedCount = (post.commentCount - 1).coerceAtLeast(0)
        _post.value = post.copy(commentCount = updatedCount)
        postUpdateNotifier.notifyPostUpdated(PostUpdate(postId = postId, commentCount = updatedCount))
    }

    override fun onCleared() {
        // 디바운스 delay 중 화면 이탈 시 viewModelScope가 취소되기 전에 펜딩 커밋을 독립 스코프로 실행 (Case A)
        // flag를 먼저 false로 리셋 → delay가 이미 만료돼 큐에 올라간 debounce 연속체가
        // if (!likePendingCommit) guard에 걸려 중복 커밋하지 않도록 방지
        if (likePendingCommit) {
            likePendingCommit = false
            likeDebounceJob?.cancel()
            CoroutineScope(SupervisorJob()).launch { commitLike() }
        }
        if (bookmarkPendingCommit) {
            bookmarkPendingCommit = false
            bookmarkDebounceJob?.cancel()
            CoroutineScope(SupervisorJob()).launch { commitBookmark() }
        }
        super.onCleared()
    }

    fun openPostReportSheet() {
        _uiState.update {
            it.copy(
                showReportSheet = true,
                reportTargetType = "post",
                reportTargetId = postId,
                selectedReportReason = null,
                reportDetailText = ""
            )
        }
    }

    fun openCommentReportSheet(commentId: String) {
        _uiState.update {
            it.copy(
                showReportSheet = true,
                reportTargetType = "comment",
                reportTargetId = commentId,
                selectedReportReason = null,
                reportDetailText = ""
            )
        }
    }

    fun closeReportSheet() {
        _uiState.update { it.copy(showReportSheet = false) }
    }

    fun selectReportReason(reason: ReportReason) {
        _uiState.update {
            it.copy(
                selectedReportReason = reason,
                reportDetailText = if (reason == ReportReason.OTHER) it.reportDetailText else ""
            )
        }
    }

    fun onReportDetailChange(text: String) {
        _uiState.update { it.copy(reportDetailText = text) }
    }

    fun submitReport() {
        val state = _uiState.value
        val reason = state.selectedReportReason ?: return
        if (state.isReportSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isReportSubmitting = true) }

            val parentPostId = if (state.reportTargetType == "comment") postId else null

            createCommunityReportUseCase(
                targetType = state.reportTargetType,
                targetId = state.reportTargetId,
                parentPostId = parentPostId,
                reason = reason,
                detail = state.reportDetailText
            )
                .onSuccess {
                    _uiState.update { it.copy(showReportSheet = false) }
                    sendToast("신고가 접수되었습니다.")
                    refresh()
                }
                .onFailure { e ->
                    when (e) {
                        is AppError.UnknownUser -> Unit
                        else -> sendToast(e.message)
                    }
                }

            _uiState.update { it.copy(isReportSubmitting = false) }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(CommunityDetailEvent.ShowToast(message)) }
    }

    private fun notifyPostChanged(post: Post) {
        postUpdateNotifier.notifyPostUpdated(
            PostUpdate(
                postId = post.postId,
                isLiked = post.isLiked,
                likeCount = post.likeCount,
                isBookmarked = post.isBookmarked,
                bookmarkCount = post.bookmarkCount,
                commentCount = post.commentCount,
                title = post.title,
                content = post.content,
                tag = post.tag,
                updatedAt = post.updatedAt
            )
        )
    }

}
