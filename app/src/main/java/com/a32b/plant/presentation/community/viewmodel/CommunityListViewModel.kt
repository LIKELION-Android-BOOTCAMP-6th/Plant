package com.a32b.plant.presentation.community.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.community.PostUpdateObserver
import com.a32b.plant.domain.community.applyUpdate
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.community.ObserveBlockedUsersUseCase
import com.a32b.plant.presentation.community.BlockedContentFilter
import com.a32b.plant.presentation.community.CommunityConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommunityListUiState(
    val tags: List<Tag> = emptyList(),
    val selected: List<Tag> = emptyList(),
    val isTagSheetShown: Boolean = false,
    val isSharedShown: Boolean = false,
    val posts: List<Post> = emptyList(),
    val cursor: Long? = null,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLoadedOnce: Boolean = false,  // LoadableScreen(최초) ↔ LoadingBox(필터 변경) 분기용
    val searchResults: List<Post> = emptyList(),  // 검색 전체 로드 결과
    val isSearchLoading: Boolean = false,           // 검색 로딩 중
    val suppressLoadMore: Boolean = false           // refresh 후 ScrollToTop 완료 전까지 loadMore 차단
) {
    // 목록 맨 아래 로딩 표시 항목을 보여 주는지 — 목록과 하단 도달 감지(배치 반영 여부 판단)가 같은 기준을 쓰도록 한 곳에 둔다
    val showsLoadingIndicator: Boolean get() = hasMore && isLoadingMore && posts.isNotEmpty()
}

sealed class CommunityListEvent {
    data class ShowToast(val message: String) : CommunityListEvent()
    object ScrollToTop : CommunityListEvent()
}

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val postUpdateObserver: PostUpdateObserver,
    private val observeBlockedUsersUseCase: ObserveBlockedUsersUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CommunityListUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _events = Channel<CommunityListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // 차단한 사용자 uid (#660). null = 아직 차단 목록을 받지 못함 (첫 로드에서 차단한 글이 잠깐 보이지 않게 기다리는 데 사용)
    // posts·searchResults에는 차단한 사용자 글도 그대로 두고 화면에 보낼 때(filteredPosts)만 뺀다
    // → 커서·silentRefresh() 재조회 개수가 서버 페이지와 그대로 맞고, 차단·해제가 다시 불러오지 않아도 바로 반영된다
    private val _blockedUids = MutableStateFlow<Set<String>?>(null)
    private val blockedUids: Set<String> get() = _blockedUids.value.orEmpty()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var searchJob: Job? = null

    // postId → 로컬 패치가 적용된 시각. TTL(LOCAL_PATCH_PROTECTION_WINDOW_MS) 내에는
    // silentRefresh()의 서버 응답이 이 패치를 덮어쓰지 않도록 보호하는 데 사용.
    // 쓰기(패치)와 읽기(쿼리) 중 어느 쪽이 서버에 먼저 반영됐는지는 클라이언트에서 판별할 수 없으므로
    // queryStartedAt과의 선후 비교 없이 순수 TTL로만 판단한다
    private val recentLocalPatches = mutableMapOf<String, Long>()
    // 마지막으로 서버와 완전히 동기화된(= posts를 서버 응답으로 통째로 교체한) 시각
    private var lastSyncedAt = 0L
    // 마지막 동기화 이후 상세·글 수정·내 활동 화면에서의 상호작용(PostUpdate)이나 새 글 등록·글 삭제가 있었는지 — 없으면 ON_RESUME 시 불필요한 재조회를 생략
    private var hasPendingInteractionSinceLastSync = false
    // 마지막 동기화 이후 새 글을 등록했는지 — silentRefresh()가 성공하면 맨 위로 스크롤해 새 글을 보여 준다 (#669).
    // 목록은 postId를 key로 스크롤 위치를 유지하므로, 스크롤하지 않으면 새 글이 기존 첫 글 위(화면 밖)에 들어간다
    private var hasCreatedPostSinceLastSync = false

    companion object {
        private const val PAGE_SIZE = CommunityConstants.POST_PAGE_SIZE
        // 로컬 패치 보호 유지 시간 — 디바운스(500ms) + 서버 쓰기 왕복을 넉넉히 덮는 안전 마진.
        // 실패한 커밋까지 영구히 보호하지 않도록 TTL로만 동작 (시간이 지나면 서버 값을 다시 신뢰)
        private const val LOCAL_PATCH_PROTECTION_WINDOW_MS = 5_000L
        // 상호작용이 전혀 없어도, 다른 사용자의 변경사항을 놓치지 않기 위한 최소 백그라운드 재동기화 주기
        private const val BACKGROUND_RESYNC_INTERVAL_MS = 60_000L
        // 첫 로드에서 차단 목록을 기다리는 최대 시간 — 보통 기기 캐시에서 바로 오고, 못 받아도 도착하면 그때 숨겨진다
        private const val BLOCKED_USERS_WAIT_MS = 2_000L
    }

    init {
        observeBlockedUsers()
        fetchTags()
        loadInitial()
        observePostUpdates()
        observePostCreated()
        observePostDeleted()
    }

    // 차단·해제는 화면에 보낼 때 다시 거르므로(filteredPosts) 목록을 다시 불러오지 않는다.
    // 상세 화면에서 차단해 지금 불러온 글이 전부 숨겨지면 화면 하단 도달 신호가 오지 않으므로 다음 페이지를 직접 부른다
    private fun observeBlockedUsers() {
        viewModelScope.launch {
            observeBlockedUsersUseCase().collect { users ->
                _blockedUids.value = users.map { it.uid }.toSet()
                val state = _uiState.value
                if (state.hasLoadedOnce && !state.isLoadingMore && !state.isRefreshing &&
                    BlockedContentFilter.needsNextPage(state.posts, state.hasMore, blockedUids)
                ) {
                    loadMore()
                }
            }
        }
    }

    // 방금 받은 페이지가 전부 차단한 사용자 글이면 다음 페이지를 이어서 부른다 (#660).
    // 다음 페이지도 전부 숨겨지면 다시 여기로 와서 끝(hasMore = false)까지 이어지고, 끝이면 빈 목록 안내가 보인다.
    // 실패하면 이어 부르지 않는다 (hasMore는 true로 남고, 빈 목록 안내에서 당겨서 새로고침으로 다시 시도할 수 있다)
    // 메인에서 부른다: loadMore()를 부르는 곳이 모두 메인이 되어, 필터 변경(loadInitial)이 첫 페이지를 불러오는 동안(isLoadingMore = true) 도착한 예전 호출은 거절된다
    private fun loadMoreIfPageHidden(pageItems: List<Post>, hasMore: Boolean) {
        viewModelScope.launch {
            if (BlockedContentFilter.needsNextPage(pageItems, hasMore, blockedUids)) loadMore()
        }
    }

    // 삭제한 글을 바로 빼서, 재조회를 건너뛰는 60초 안의 복귀에서도 남지 않게 한다 (#712).
    // 검색·필터 조건은 건드리지 않고 그 글만 뺀다. 다음 복귀 때 silentRefresh()로 커서·hasMore를 서버 값으로 맞춘다
    // (silentRefresh()는 남은 글 수만큼 다시 받으므로, 빈자리는 남은 글이 한 페이지 미만일 때만 채워지고 그 밖에는 loadMore()가 이어 받는다)
    private fun observePostDeleted() {
        viewModelScope.launch {
            postUpdateObserver.postDeleted.collect { postId ->
                recentLocalPatches.remove(postId)
                hasPendingInteractionSinceLastSync = true
                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.filterNot { it.postId == postId },
                        searchResults = state.searchResults.filterNot { it.postId == postId }
                    )
                }
            }
        }
    }

    // 새 글은 목록에 없어 로컬 패치할 수 없으므로, 다음 화면 복귀 때 silentRefresh()로 다시 불러오게 한다 (#669)
    private fun observePostCreated() {
        viewModelScope.launch {
            postUpdateObserver.postCreated.collect {
                hasPendingInteractionSinceLastSync = true
                hasCreatedPostSinceLastSync = true
            }
        }
    }

    // 상세·글 수정·내 활동 화면에서의 상호작용 결과를 목록 화면에 즉시 반영 (서버 재조회 없이 로컬 패치)
    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateObserver.event.collect { update ->
                val now = System.currentTimeMillis()
                recentLocalPatches[update.postId] = now
                // 만료된 기록 정리 — map이 무한정 커지는 것 방지
                recentLocalPatches.entries.removeAll { now - it.value >= LOCAL_PATCH_PROTECTION_WINDOW_MS }
                hasPendingInteractionSinceLastSync = true

                _uiState.update { state ->
                    state.copy(
                        posts = state.posts.map { post ->
                            if (post.postId == update.postId) post.applyUpdate(update) else post
                        },
                        searchResults = state.searchResults.map { post ->
                            if (post.postId == update.postId) post.applyUpdate(update) else post
                        }
                    )
                }
            }
        }
    }

    // silentRefresh() 전용: TTL 내에 로컬 패치가 적용된 post는 서버의 오래된 응답으로 덮어쓰지 않고 현재 값을 유지
    private fun mergeWithLocalPatches(serverPosts: List<Post>, currentPosts: List<Post>): List<Post> {
        val now = System.currentTimeMillis()
        return serverPosts.map { serverPost ->
            val patchedAt = recentLocalPatches[serverPost.postId]
            val isProtected = patchedAt != null && now - patchedAt < LOCAL_PATCH_PROTECTION_WINDOW_MS
            if (isProtected) {
                currentPosts.find { it.postId == serverPost.postId } ?: serverPost
            } else {
                serverPost
            }
        }
    }

    // 태그 목록 조회 (필터 시트용)
    private fun fetchTags() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getTags()
                .onSuccess { tags -> _uiState.update { it.copy(tags = tags) } }
                .onFailure { e -> Log.e("CommunityListVM", "태그 목록 조회 실패: ${e.message}") }
        }
    }

    // 필터 상태 기반으로 첫 페이지부터 새로 가져온다
    private fun loadInitial() {
        loadJob?.cancel()
        loadMoreJob?.cancel()
        // 코루틴을 띄우기 전에 바꾼다 (#660) — 그 사이에 loadMore()가 예전 상태로 시작해 첫 페이지를 두 번 붙이지 않게 하고,
        // 위에서 취소한 refresh()가 isRefreshing = true를 남기지 않게 함께 되돌린다
        _uiState.update {
            it.copy(isLoadingMore = true, isRefreshing = false, cursor = null, hasMore = true, posts = emptyList())
        }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            // withTimeoutOrNull: 오프라인 + 캐시 없을 때 Firestore가 무한 대기하는 현상 방지
            val pageResult = withTimeoutOrNull(10_000L) {
                repository.loadPostPage(
                    cursor = null,
                    pageSize = PAGE_SIZE,
                    tagIds = state.selected.map { it.id },
                    sharedOnly = state.isSharedShown
                )
            } ?: Result.Failure(AppError.Network())

            // 차단 목록을 받기 전에 글을 보여 주면 차단한 사용자 글이 잠깐 보였다 사라지므로 잠시 기다린다
            withTimeoutOrNull(BLOCKED_USERS_WAIT_MS) { _blockedUids.filterNotNull().first() }

            pageResult.onSuccess { page ->
                Log.d("CommunityListVM", "loadInitial 성공: ${page.items.size}개 로드, hasMore=${page.hasMore}, nextCursor=${page.nextCursor}")
                lastSyncedAt = System.currentTimeMillis()
                hasPendingInteractionSinceLastSync = false
                hasCreatedPostSinceLastSync = false
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                        hasLoadedOnce = true
                    )
                }
                loadMoreIfPageHidden(page.items, page.hasMore)
            }.onFailure { e ->
                Log.e("CommunityListVM", "loadInitial 실패: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMore = false, hasMore = false, hasLoadedOnce = true) }
                _events.trySend(CommunityListEvent.ShowToast(e.message ?: "게시글 목록을 불러오지 못했습니다."))
            }

            loaded() // BaseViewModel: 스플래시 종료 (성공/실패 모두 최초 시도 후 종료)
        }
    }

    // 스크롤로 하단 도달 시 호출. 이미 로딩 중이거나 hasMore=false면 무시
    fun loadMore() {
        // 확인과 isLoadingMore = true를 update 한 번으로 한다 — 하단 감지·차단 목록 변경·빈 페이지 이어 부르기가 이어서 불러도
        // 하나만 시작해 같은 페이지를 두 번 붙이지 않는다 (같은 postId가 두 번 생기면 목록 key가 겹쳐 앱이 종료됨, #660)
        var started = false
        _uiState.update { state ->
            started = !(state.isLoadingMore || state.isRefreshing || !state.hasMore)
            if (started) state.copy(isLoadingMore = true) else state
        }
        if (!started) return

        loadMoreJob = viewModelScope.launch(Dispatchers.IO) {
            val curState = _uiState.value

            val pageResult = withTimeoutOrNull(10_000L) {
                repository.loadPostPage(
                    cursor = curState.cursor,
                    pageSize = PAGE_SIZE,
                    tagIds = curState.selected.map { it.id },
                    sharedOnly = curState.isSharedShown
                )
            } ?: Result.Failure(AppError.Network())

            pageResult.onSuccess { page ->
                Log.d("CommunityListVM", "loadMore 성공: ${page.items.size}개 추가, 총 ${curState.posts.size + page.items.size}개, hasMore=${page.hasMore}")
                _uiState.update {
                    it.copy(
                        // 취소가 늦게 닿은 이전 요청 등으로 이미 있는 글이 다시 오면 빼서, 목록 key가 겹치지 않게 한다 (#660)
                        posts = (it.posts + page.items).distinctBy { post -> post.postId },
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
                loadMoreIfPageHidden(page.items, page.hasMore)
            }.onFailure { e ->
                Log.e("CommunityListVM", "loadMore 실패: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMore = false) }
                // hasMore는 true 유지 → 사용자가 다시 스크롤해 재시도 가능 (화면을 채우지 못하는 짧은 목록은 당겨서 새로고침으로)
            }
        }
    }

    // Pull-to-refresh: 필터 유지, 첫 페이지부터 재수신
    fun refresh() {
        Log.d("CommunityListVM", "refresh() 호출됨, isRefreshing=${_uiState.value.isRefreshing}")
        // 확인과 isRefreshing = true를 update 한 번으로 하고, 코루틴을 띄우기 전에 바꾼다 — 그 사이에 loadMore()가 시작되지 않게 한다 (#660).
        // 아래에서 취소하는 loadMore가 isLoadingMore = true를 남길 수 있으므로 되돌린다.
        // 단 글이 비어 있으면(필터 재조회 중) 그대로 둬, 새로고침이 끝날 때까지 로딩 표시 대신 "글 없음"이 보이지 않게 한다
        var started = false
        _uiState.update { state ->
            started = !state.isRefreshing
            if (started) state.copy(isRefreshing = true, isLoadingMore = state.posts.isEmpty() && state.isLoadingMore) else state
        }
        if (!started) return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            Log.d("CommunityListVM", "refresh 시작")
            val state = _uiState.value

            val pageResult = withTimeoutOrNull(10_000L) {
                repository.loadPostPage(
                    cursor = null,
                    pageSize = PAGE_SIZE,
                    tagIds = state.selected.map { it.id },
                    sharedOnly = state.isSharedShown
                )
            } ?: Result.Failure(AppError.Network())

            pageResult.onSuccess { page ->
                lastSyncedAt = System.currentTimeMillis()
                hasPendingInteractionSinceLastSync = false
                hasCreatedPostSinceLastSync = false  // 아래 ScrollToTop으로 새 글도 함께 보인다
                // suppressLoadMore=true 먼저 세팅 → recomposition 타이밍에 loadMore 끼어들기 원천 차단
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isRefreshing = false,
                        isLoadingMore = false,
                        suppressLoadMore = true
                    )
                }
                _events.trySend(CommunityListEvent.ScrollToTop)
                loadMoreIfPageHidden(page.items, page.hasMore)
            }.onFailure { e ->
                Log.e("CommunityListVM", "refresh 실패: ${e.message}", e)
                _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false) }
                _events.trySend(CommunityListEvent.ShowToast(e.message ?: "새로고침에 실패했습니다."))
            }
        }
    }

    // 태그 선택 변경 → 서버 재쿼리
    // Firestore whereIn 제한: 최대 10개 → 초과 시 UI에서 차단
    fun onSelectedChanged(tags: List<Tag>) {
        if (tags.size > 10) {
            _events.trySend(CommunityListEvent.ShowToast("태그는 최대 10개까지만 선택할 수 있어요."))
            return
        }
        _uiState.update { it.copy(selected = tags) }
        loadInitial()
    }

    // 공유글 스위치 토글 → 서버 재쿼리
    fun onSharedShownChange() {
        _uiState.update { it.copy(isSharedShown = !it.isSharedShown) }
        loadInitial()
    }

    // 검색어 변경: 비어있으면 페이지네이션 모드 복귀, 있으면 300ms 디바운스 후 전체 로드
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.isBlank()) {
            // 검색 모드 해제 → 기존 페이지네이션 목록으로 복귀
            _uiState.update { it.copy(searchResults = emptyList(), isSearchLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // 타이핑 중 불필요한 요청 방지
            _uiState.update { it.copy(isSearchLoading = true) }

            val state = _uiState.value
            repository.getAllPosts(
                tagIds = state.selected.map { it.id },
                sharedOnly = state.isSharedShown
            ).onSuccess { posts ->
                _uiState.update { it.copy(searchResults = posts, isSearchLoading = false) }
            }.onFailure { e ->
                Log.e("CommunityListVM", "검색 실패: ${e.message}", e)
                _uiState.update { it.copy(isSearchLoading = false) }
                _events.trySend(CommunityListEvent.ShowToast(e.message ?: "검색에 실패했습니다."))
            }
        }
    }

    // ScrollToTop 애니메이션 완료 후 Screen에서 호출 → loadMore 차단 해제
    fun clearSuppressLoadMore() {
        _uiState.update { it.copy(suppressLoadMore = false) }
    }

    // 화면 복귀(Resume) 시 호출 — 초기 로딩 전에는 무시
    // 마지막 동기화 이후 상호작용이 없었다면(= 상세·글 수정·내 활동 화면에서 아무것도 바뀌지 않았고 새 글 등록·글 삭제도 없었다면) 불필요한 재조회를 생략.
    // 단, 다른 사용자의 변경사항을 영원히 놓치지 않도록 일정 주기(BACKGROUND_RESYNC_INTERVAL_MS)마다는 강제로 재동기화
    fun refreshOnResume() {
        if (!_uiState.value.hasLoadedOnce) return
        val dueForBackgroundSync = System.currentTimeMillis() - lastSyncedAt >= BACKGROUND_RESYNC_INTERVAL_MS
        if (hasPendingInteractionSinceLastSync || dueForBackgroundSync) {
            silentRefresh()
        }
    }

    // PullToRefresh 인디케이터 없이 백그라운드로 조용히 갱신 (화면 복귀 전용)
    private fun silentRefresh() {
        if (_uiState.value.isRefreshing) return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        // loadMoreJob 취소 시 코루틴이 CancellationException으로 종료되어 onFailure가 실행되지 않음
        // → isLoadingMore=true가 고착될 수 있으므로 명시적으로 초기화
        _uiState.update { it.copy(isLoadingMore = false) }
        hasPendingInteractionSinceLastSync = false
        // 시작 시점에 꺼내 둔다 — 실패하면 다시 켜서 다음 복귀 때 재시도한다
        val scrollToTopAfterSync = hasCreatedPostSinceLastSync
        hasCreatedPostSinceLastSync = false
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            // 현재 로드된 게시글 수만큼 재로드해 스크롤 위치와 목록을 보존
            val refreshSize = maxOf(state.posts.size, PAGE_SIZE)
            val pageResult = withTimeoutOrNull(10_000L) {
                repository.loadPostPage(
                    cursor = null,
                    pageSize = refreshSize,
                    tagIds = state.selected.map { it.id },
                    sharedOnly = state.isSharedShown
                )
            }

            if (pageResult == null) {
                // 타임아웃 → 이번 동기화는 실패한 것이므로 다음 ON_RESUME에서 재시도할 수 있도록 플래그 복구
                hasPendingInteractionSinceLastSync = true
                if (scrollToTopAfterSync) hasCreatedPostSinceLastSync = true
                return@launch
            }

            pageResult.onSuccess { page ->
                lastSyncedAt = System.currentTimeMillis()
                _uiState.update { current ->
                    current.copy(
                        posts = mergeWithLocalPatches(page.items, current.posts),
                        cursor = page.nextCursor,
                        hasMore = page.hasMore
                    )
                }
                if (scrollToTopAfterSync) _events.trySend(CommunityListEvent.ScrollToTop)
                loadMoreIfPageHidden(page.items, page.hasMore)
            }.onFailure {
                // 네트워크 실패 → 다음 ON_RESUME에서 재시도할 수 있도록 플래그 복구
                hasPendingInteractionSinceLastSync = true
                if (scrollToTopAfterSync) hasCreatedPostSinceLastSync = true
            }
        }
    }

    fun onIsTagSheetShownChange() = _uiState.update { it.copy(isTagSheetShown = !it.isTagSheetShown) }

    // 화면에 표시할 리스트 — 차단한 사용자 글은 두 경우 모두 뺀다 (#660)
    // 검색어 없음 → 페이지네이션 posts 그대로
    // 검색어 있음 → 전체 로드된 searchResults에서 contains 필터 (블라인드 게시글은 숨겨진 제목/본문으로 매칭되지 않도록 제외)
    val filteredPosts = combine(
        _uiState,
        _searchQuery,
        _blockedUids
    ) { state, query, blocked ->
        val blockedUids = blocked.orEmpty()
        if (query.isBlank()) {
            BlockedContentFilter.visiblePosts(state.posts, blockedUids)
        } else {
            BlockedContentFilter.visiblePosts(state.searchResults, blockedUids).filter { post ->
                !post.isBlind && (
                    post.title.contains(query, ignoreCase = true) ||
                        (post.content?.contains(query, ignoreCase = true) ?: false)
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
