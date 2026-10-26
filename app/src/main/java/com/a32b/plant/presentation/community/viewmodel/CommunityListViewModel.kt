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
)

sealed class CommunityListEvent {
    data class ShowToast(val message: String) : CommunityListEvent()
    object ScrollToTop : CommunityListEvent()
}

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val repository: CommunityRepository,
    private val postUpdateObserver: PostUpdateObserver
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(CommunityListUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _events = Channel<CommunityListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var searchJob: Job? = null

    companion object {
        private const val PAGE_SIZE = CommunityConstants.POST_PAGE_SIZE
    }

    init {
        fetchTags()
        loadInitial()
        observePostUpdates()
    }

    // 상세 화면에서의 상호작용 결과를 목록 화면에 즉시 반영 (서버 재조회 없이 로컬 패치)
    private fun observePostUpdates() {
        viewModelScope.launch {
            postUpdateObserver.event.collect { update ->
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
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingMore = true, cursor = null, hasMore = true, posts = emptyList()) }

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

            pageResult.onSuccess { page ->
                Log.d("CommunityListVM", "loadInitial 성공: ${page.items.size}개 로드, hasMore=${page.hasMore}, nextCursor=${page.nextCursor}")
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isLoadingMore = false,
                        hasLoadedOnce = true
                    )
                }
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
        val state = _uiState.value
        if (state.isLoadingMore || state.isRefreshing || !state.hasMore) return

        loadMoreJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoadingMore = true) }
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
                        posts = it.posts + page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
            }.onFailure { e ->
                Log.e("CommunityListVM", "loadMore 실패: ${e.message}", e)
                _uiState.update { it.copy(isLoadingMore = false) }
                // hasMore는 true 유지 → 사용자가 다시 스크롤해 재시도 가능
            }
        }
    }

    // Pull-to-refresh: 필터 유지, 첫 페이지부터 재수신
    fun refresh() {
        Log.d("CommunityListVM", "refresh() 호출됨, isRefreshing=${_uiState.value.isRefreshing}")
        if (_uiState.value.isRefreshing) return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isRefreshing = true) }
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
                // suppressLoadMore=true 먼저 세팅 → recomposition 타이밍에 loadMore 끼어들기 원천 차단
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isRefreshing = false,
                        suppressLoadMore = true
                    )
                }
                _events.trySend(CommunityListEvent.ScrollToTop)
            }.onFailure { e ->
                Log.e("CommunityListVM", "refresh 실패: ${e.message}", e)
                _uiState.update { it.copy(isRefreshing = false) }
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
    fun refreshOnResume() {
        if (_uiState.value.hasLoadedOnce) silentRefresh()
    }

    // PullToRefresh 인디케이터 없이 백그라운드로 조용히 갱신 (화면 복귀 전용)
    private fun silentRefresh() {
        if (_uiState.value.isRefreshing) return
        loadJob?.cancel()
        loadMoreJob?.cancel()
        // loadMoreJob 취소 시 코루틴이 CancellationException으로 종료되어 onFailure가 실행되지 않음
        // → isLoadingMore=true가 고착될 수 있으므로 명시적으로 초기화
        _uiState.update { it.copy(isLoadingMore = false) }
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
            } ?: return@launch

            pageResult.onSuccess { page ->
                _uiState.update {
                    it.copy(
                        posts = page.items,
                        cursor = page.nextCursor,
                        hasMore = page.hasMore
                    )
                }
            }
        }
    }

    fun onIsTagSheetShownChange() = _uiState.update { it.copy(isTagSheetShown = !it.isTagSheetShown) }

    // 화면에 표시할 리스트
    // 검색어 없음 → 페이지네이션 posts 그대로
    // 검색어 있음 → 전체 로드된 searchResults에서 contains 필터
    val filteredPosts = combine(
        _uiState,
        _searchQuery
    ) { state, query ->
        if (query.isBlank()) {
            state.posts
        } else {
            state.searchResults.filter { post ->
                post.title.contains(query, ignoreCase = true) ||
                    (post.content?.contains(query, ignoreCase = true) ?: false)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}
