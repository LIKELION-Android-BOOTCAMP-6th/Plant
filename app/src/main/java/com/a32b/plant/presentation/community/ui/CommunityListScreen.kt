package com.a32b.plant.presentation.community.ui

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.compose.dropUnlessStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.Post
import com.a32b.plant.presentation.community.CommunityConstants
import com.a32b.plant.presentation.community.viewmodel.CommunityListEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityListViewModel
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.TabTopBar
import com.a32b.plant.presentation.core.component.TagChip
import com.a32b.plant.presentation.core.component.TagSheet
import com.a32b.plant.presentation.core.extension.showToast
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(
    navController: NavController,
    viewModel: CommunityListViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val listState = rememberLazyListState()

    // 상세/활동 화면에서 복귀 시 목록 새로고침
    // LifecycleEventObserver는 등록 시점에 현재 lifecycle 상태까지 이벤트를 즉시 재생하므로
    // 초기 진입 시에도 ON_RESUME이 발화됨 — refreshOnResume() 내 hasLoadedOnce 가드로 무시됨
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 이벤트 수신 (토스트, 맨 위로 스크롤)
    // ScrollToTop: 맨 위 스크롤이 끝나거나 건너뛰거나 중단되면 clearSuppressLoadMore() 호출 → loadMore 차단 해제
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityListEvent.ShowToast -> context.showToast(event.message)
                is CommunityListEvent.ScrollToTop -> {
                    try {
                        // 목록이 한 번도 배치되지 않았으면(빈 목록 안내만 보이는 중) 첫 배치까지 기다려 버리므로 건너뛰고,
                        // 그 밖에 끝나지 않는 경우에 대비해 시간을 제한한다 — 기다리는 동안 이후 이벤트와 loadMore 차단 해제가 막힌다 (#660)
                        if (listState.layoutInfo.totalItemsCount > 0) {
                            withTimeoutOrNull(SCROLL_TO_TOP_TIMEOUT_MS) { listState.animateScrollToItem(0) }
                        }
                    } catch (e: CancellationException) {
                        // 이 코루틴이 살아 있는데 난 취소(애니메이션 중 사용자가 목록을 만짐, 이미 드래그 중이라 시작하지 못함 등)는 넘어간다 (#660).
                        // 넘어가지 않으면 이벤트 수신이 끝나 이후 토스트·맨 위 스크롤을 받지 못하고, loadMore 차단도 풀리지 않는다.
                        // 화면을 떠나 이 코루틴 자체가 취소된 경우는 그대로 끝낸다
                        currentCoroutineContext().ensureActive()
                    } finally {
                        viewModel.clearSuppressLoadMore()  // 스크롤이 끝나거나 중단되면 loadMore 재허용
                    }
                }
            }
        }
    }

    LoadableScreen(viewModel) {
        // State<T> 참조 보존 (collect 핸들러에서 suppressLoadMore 최신값 읽기 위해)
        val uiStateFlow = viewModel.uiState.collectAsStateWithLifecycle()
        val uiState by uiStateFlow
        val filteredPosts by viewModel.filteredPosts.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

        // 하단 도달 감지 → loadMore
        // snapshotFlow: 스크롤 상태 변화를 레이아웃 단계에서 직접 관찰 (derivedStateOf보다 안정적)
        // 하단 근처면 지금 보이는 글 수(filteredPosts), 아니면 -1을 보낸다 (#660).
        // - 보이는 글 수가 바뀌면 다시 보내므로, 받은 페이지 일부가 차단한 사용자 글이라 목록이 화면을 채우지 못해도 이어 부른다.
        //   전부 차단한 사용자 글인 페이지는 보이는 글 수가 그대로라 여기서는 부르지 않고 ViewModel이 이어 부른다(loadMoreIfPageHidden)
        // - 로딩 표시 항목은 값에도, 하단 판단에도 세지 않는다 (아래 하단 판단 주석 참고)
        // - 항목 수가 지금 목록과 다르거나, 이미 사라진 로딩 표시가 배치에 아직 보이면 보내지 않는다(null):
        //   반영 전 배치로 판단하는 경우를 줄인다
        // - 검색 중에는 -1: 검색 결과는 전체 로드(searchResults)라 페이지가 필요 없다
        // - 새로고침 뒤 맨 위 스크롤이 끝날 때까지(suppressLoadMore)도 -1: 막힌 동안 값을 보내면 풀린 뒤 같은 값이라 다시 보내지 않는다
        LaunchedEffect(listState) {
            snapshotFlow {
                val state = uiStateFlow.value
                if (searchQuery.isNotBlank() || state.suppressLoadMore) return@snapshotFlow -1
                val info = listState.layoutInfo
                // 아래 LazyColumn의 항목 구성(보이는 글 + 로딩 표시)과 같아야 한다 — 항목을 추가하면 여기도 함께 바꿀 것
                val expectedItemCount = filteredPosts.size + if (state.showsLoadingIndicator) 1 else 0
                val layoutHasIndicator = info.visibleItemsInfo.any { it.key == LOADING_INDICATOR_KEY }
                if (info.totalItemsCount != expectedItemCount ||
                    (layoutHasIndicator && !state.showsLoadingIndicator)
                ) return@snapshotFlow null
                val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow -1
                // 하단 근처는 글 수 기준으로 판단한다 — 로딩 표시 항목까지 세면 그 항목이 생겼다 사라질 때 하단 판단이
                // 꺼졌다 켜져(-1 → 같은 글 수), 불러오기가 끝나자마자 다음 페이지를 한 번 더 부른다 (성공한 경우를 실기기 로그로 확인).
                // 실패해 로딩 표시가 사라질 때도 같은 이유로 다시 부르지 않는다
                val postCount = filteredPosts.size
                if (postCount > 0 && lastVisible >= postCount - 3) postCount else -1
            }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { nearBottomCount ->
                    if (nearBottomCount >= 0 && !uiStateFlow.value.suppressLoadMore) {
                        viewModel.loadMore()
                    }
                }
        }

        // 당겨서 새로고침 + 화면 콘텐츠 영역
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        Column(
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        ) {
                            TabTopBar(title = "커뮤니티") {
                                TextButton(
                                    onClick = dropUnlessStarted { navController.navigate(Routes.CommunityActivity) }
                                ) {
                                    Text(
                                        "내 활동",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                SearchBarSection(
                                    query = searchQuery,
                                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                                )
                                TagFilterRow(
                                    isTagSheetShown = uiState.isTagSheetShown,
                                    isSharedShown = uiState.isSharedShown,
                                    onTagClick = { viewModel.onIsTagSheetShownChange() },
                                    onClearClick = { viewModel.onSelectedChanged(emptyList()) },
                                    onSharedShownChange = { viewModel.onSharedShownChange() }
                                )
                                if (uiState.selected.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp, start = 16.dp, end = 16.dp),
                                        maxItemsInEachRow = 6,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        uiState.selected.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding())
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            if (uiState.posts.isEmpty() && !uiState.isLoadingMore) {
                                EmptyStateView()
                            } else {
                                if (filteredPosts.isEmpty() && !uiState.isLoadingMore) {
                                    EmptyStateView(hasQuery = searchQuery.isNotBlank())
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(filteredPosts, key = { it.postId }) { post ->
                                            PostCard(
                                                post = post,
                                                isLiked = post.isLiked,
                                                isBookmarked = post.isBookmarked,
                                                onClick = dropUnlessStarted {
                                                    navController.navigate(
                                                        Routes.CommunityDetail(
                                                            postId = post.postId
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                        // 하단 로딩 인디케이터 (loadMore 중, 데이터 있을 때만)
                                        if (uiState.showsLoadingIndicator) {
                                            item(key = LOADING_INDICATOR_KEY) {
                                                LoadingBox(modifier = Modifier.fillMaxWidth().padding(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 레이어 순서: 게시글 목록 < 바깥 클릭 레이어(1) < FAB(2) < 로딩 오버레이(3) < 태그 선택 창(4)
                        // 태그 선택 창 오버레이 (게시글 목록을 밀지 않고 위에 덮음)
                        if (uiState.isTagSheetShown) {
                            // 바깥 클릭 시 닫기용 투명 레이어
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(1f)
                                    .clickable { viewModel.onIsTagSheetShownChange() }
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = innerPadding.calculateTopPadding())
                                    .padding(horizontal = 20.dp)
                                    .fillMaxWidth()
                                    .zIndex(4f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.background,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    TagSheet(
                                        uiState.tags,
                                        isMultiSelected = true,
                                        init = uiState.selected
                                    ) { selected ->
                                        viewModel.onSelectedChanged(selected.toList())
                                    }
                                }
                            }
                        }

                        // 로딩 오버레이가 덮어 클릭을 막을 수 있도록 같은 Box 안에 배치
                        FloatingActionButton(
                            onClick = dropUnlessStarted { navController.navigate(Routes.CommunityPost()) },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                                .zIndex(2f)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }

                        // 필터 변경 재쿼리 로딩 오버레이
                        if (uiState.hasLoadedOnce && uiState.isLoadingMore && uiState.posts.isEmpty()) {
                            LoadingBox(modifier = Modifier.fillMaxSize().zIndex(3f))
                        }
                        // 검색 전체 로드 중 오버레이
                        if (uiState.isSearchLoading) {
                            LoadingBox(modifier = Modifier.fillMaxSize().zIndex(3f))
                        }
                    }
                }
            }
        }
    }
}

/** [TagFilterRow]의 공유글만 보기 스위치 (테스트에서 스위치 모양의 자리를 찾을 때 씀) */
internal const val SHARED_SWITCH_TAG = "shared_switch"

/**
 * 검색창 아래 "태그 · 전체 선택 해제 · 공유글만 보기 · 스위치" 줄 (#742)
 *
 * - 네 요소가 한 줄에 다 들어가면 한 줄: 앞쪽(태그, 전체 선택 해제)은 왼쪽부터, 뒤쪽(공유글만 보기, 스위치)은 오른쪽 끝에 붙이고,
 *   요소마다 줄 높이의 가운데에 놓는다 (뒤쪽은 묶음 Row를 줄 가운데에, 글자·스위치를 Row 가운데에 놓는다).
 *   글꼴 1.0·폭 360·411dp에서 수정 전 Row(CenterVertically + 사이 Spacer weight)에 같은 글자를 넣었을 때와 위치·크기가 px 단위로 같음을
 *   TagFilterRowLargeFontTest로 확인
 * - 안 들어가면(큰 글꼴·좁은 화면) 뒤쪽을 다음 줄 오른쪽 끝으로 내린다.
 *   수정 전에는 남은 폭이 모자라 스위치 자리가 줄고 52dp 트랙이 그 자리 밖으로 넘쳐 그려져,
 *   "공유글 보기"(당시 문구) 끝 글자가 스위치에 가려지거나, 그 글자가 두 줄로 줄바꿈되고 스위치 오른쪽이 화면 밖으로 잘렸다
 * - 앞쪽 두 요소와 뒤쪽 묶음(공유글만 보기 + 스위치 Row)은 남은 폭이 아니라 줄 폭 전체를 받아 측정한다
 *   → 다음 줄로 내려간 "공유글만 보기"도 한 줄로 그려진다. 뒤쪽 묶음 안에서 스위치는 글자를 뺀 폭을 받는다 (#750)
 *   (TagFilterRowLargeFontTest: 폭 360·411dp, 글꼴 1.0·1.15·1.3·1.5·1.8·2.0에서 글자 한 줄·스위치 폭 안 줄어듦 확인)
 * - "공유글만 보기" 글자와 스위치는 한 묶음으로 누른다 (#750). 수정 전에는 Switch에 넘긴 scale(0.7f)이
 *   Switch의 누르는 범위까지 함께 줄여 48dp보다 작았고, 스위치에 라벨도 없었다
 *   (누르는 범위 가로·세로 48dp 이상, 라벨은 TagFilterRowLargeFontTest로 확인)
 */
@Composable
internal fun TagFilterRow(
    isTagSheetShown: Boolean,
    isSharedShown: Boolean,
    onTagClick: () -> Unit,
    onClearClick: () -> Unit,
    onSharedShownChange: () -> Unit,
) {
    val leading = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable(onClick = onTagClick)
        ) {
            Text(
                "태그",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                painter = painterResource(id = if (isTagSheetShown) R.drawable.ic_up else R.drawable.ic_down),
                contentDescription = "태그박스",
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.clickable(onClick = onClearClick)) {
            TagChip("전체 선택 해제", false)
        }
    }
    val trailing = @Composable {
        // 글자와 스위치를 한 번에 누르는 범위로 묶는다 (#750)
        // - 범위: 스위치 자리(52 × 48dp + 오른쪽 10dp)와 글자 전체. scale(0.7f) 밖이라 줄어들지 않는다
        // - 접근성: 이 노드 하나에 "공유글만 보기" 글자·role = Switch·켜짐/꺼짐이 함께 담긴다
        // - 줄 전체 ripple은 끄고(indication = null) 같은 interactionSource를 스위치에 넘겨, 눌림 효과는 thumb에만 나오게 한다
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.toggleable(
                value = isSharedShown,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = { onSharedShownChange() }
            )
        ) {
            Text(
                "공유글만 보기",
                style = MaterialTheme.typography.titleMedium
            )
            // onCheckedChange = null이면 Switch가 자기 누르는 범위·semantics를 만들지 않는다.
            // 그때 빠지는 minimumInteractiveComponentSize(48dp)를 직접 붙여 수정 전과 같은 자리(52 × 48dp)를 차지하게 한다
            Switch(
                isSharedShown,
                onCheckedChange = null,
                modifier = Modifier
                    .scale(0.7f)
                    .padding(end = 10.dp)
                    .minimumInteractiveComponentSize()
                    .testTag(SHARED_SWITCH_TAG),
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline
                ),
                interactionSource = interactionSource
            )
        }
    }
    Layout(
        contents = listOf(leading, trailing),
        modifier = Modifier.fillMaxWidth()
    ) { (leadingMeasurables, trailingMeasurables), constraints ->
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val leadingPlaceables = leadingMeasurables.map { it.measure(childConstraints) }
        val trailingPlaceables = trailingMeasurables.map { it.measure(childConstraints) }
        val leadingWidth = leadingPlaceables.sumOf { it.width }
        val trailingWidth = trailingPlaceables.sumOf { it.width }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else leadingWidth + trailingWidth
        val leadingHeight = leadingPlaceables.maxOf { it.height }
        val trailingHeight = trailingPlaceables.maxOf { it.height }

        val isOneLine = leadingWidth + trailingWidth <= width
        // 한 줄이면 두 묶음이 같은 줄 높이를, 두 줄이면 묶음마다 자기 줄 높이를 쓴다
        val leadingLineHeight = if (isOneLine) maxOf(leadingHeight, trailingHeight) else leadingHeight
        val trailingLineHeight = if (isOneLine) leadingLineHeight else trailingHeight
        val trailingTop = if (isOneLine) 0 else leadingLineHeight
        val height = (trailingTop + trailingLineHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(width, height) {
            var x = 0
            leadingPlaceables.forEach {
                it.placeRelative(x, Alignment.CenterVertically.align(it.height, leadingLineHeight))
                x += it.width
            }
            x = width - trailingWidth
            trailingPlaceables.forEach {
                it.placeRelative(x, trailingTop + Alignment.CenterVertically.align(it.height, trailingLineHeight))
                x += it.width
            }
        }
    }
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    val placeholderStyle = MaterialTheme.typography.labelMedium
    val inputStyle = MaterialTheme.typography.bodyMedium
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            // 입력 글자(singleLine)처럼 한 줄로 두고, 좁은 화면·큰 글꼴에서 안 들어가면 말줄임 (#749)
            Text(
                "검색어를 입력하세요",
                style = placeholderStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(searchBarHeight(placeholderStyle, inputStyle, query)),
        shape = RoundedCornerShape(12.dp),
        textStyle = inputStyle,
        trailingIcon = {
            if (query.isNotEmpty()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_community_clear),
                    contentDescription = "초기화",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                        .clickable {
                            onQueryChange("")
                            focus.clearFocus()
                        },
                    tint = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = "검색",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}

/**
 * 검색창 높이 (#742)
 *
 * OutlinedTextField는 라벨·보조 글(supportingText)이 없으면 높이에서 위아래 기본 여백(16dp씩)을 뺀 만큼만 글자 칸으로 준다.
 * 48dp로 고정하면 글자 칸이 16dp라, 큰 글꼴에서 안내 문구·입력 글자 아래쪽이 잘렸다
 * (SM-N986N 계측 테스트: 폭 360dp에서 입력 글자는 글꼴 1.15부터 높이가 넘침).
 * - 한 줄 글자 높이(안내 문구·입력 글자 중 큰 쪽)가 16dp 칸에 들어가면 수정 전과 같은 48dp (글꼴 1.0)
 * - 안 들어가면 글자 높이 + 위아래 여백으로 늘린다
 * 최소 높이(heightIn(min = 48.dp))로 두지 않는 이유: 글꼴 1.0에서도 48dp보다 높아진다
 * (SM-N986N, 폭 360dp, 빈 검색어에서 126 → 147px)
 *
 * 입력 글자는 실제 검색어로 잰다 (#749). 앱 글꼴에 없는 문자(이모지 등)는 기기의 다른 글꼴로 그려져
 * 같은 글자 크기에서도 한 줄이 더 높아질 수 있어, "검" 한 글자로만 재면 넘쳤다
 * (SM-N986N, 폭 360dp, 글꼴 1.0: "검색어 가나다" 39px, "검색😀" 46px).
 * - 앞에 "검"을 붙여 재므로 높이는 빈 검색어보다 낮아지지 않는다. 한글 검색어("검색어 가나다")는 빈 검색어와 같았다 (SearchBarSectionLargeFontTest, 글꼴 1.0이면 48dp)
 * - 더 높은 줄이 되는 문자를 넣으면 검색창이 높아지고, 지우면 돌아온다 (상단바 높이가 바뀌어 아래 목록도 그만큼 움직임)
 * - 안내 문구는 한 줄(maxLines = 1)이라 "검"으로 잰다
 */
@Composable
private fun searchBarHeight(placeholderStyle: TextStyle, inputStyle: TextStyle, query: String): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    return remember(density, placeholderStyle, inputStyle, query) {
        with(density) {
            val textHeightPx = maxOf(
                textMeasurer.measure(SEARCH_BAR_SAMPLE_TEXT, placeholderStyle, maxLines = 1).size.height,
                // 검색창이 singleLine이라 줄바꿈 없이 한 줄로 잰다
                textMeasurer.measure(
                    SEARCH_BAR_SAMPLE_TEXT + query,
                    inputStyle,
                    softWrap = false,
                    maxLines = 1
                ).size.height
            )
            // 여백도 OutlinedTextField와 같이 px로 반올림해 더한다 (dp로 더하면 반올림 차이로 1px 모자랄 수 있음)
            val verticalPaddingPx = SEARCH_BAR_VERTICAL_PADDING.roundToPx() * 2
            maxOf(SEARCH_BAR_HEIGHT.roundToPx(), textHeightPx + verticalPaddingPx).toDp()
        }
    }
}

private val SEARCH_BAR_HEIGHT = 48.dp
// OutlinedTextFieldDefaults.contentPadding()의 위·아래 기본값
private val SEARCH_BAR_VERTICAL_PADDING = 16.dp
// 한 줄 높이를 재는 글자 — 앱 글꼴(한글 포함)로 그려지는 글자
private const val SEARCH_BAR_SAMPLE_TEXT = "검"

@Composable
fun PostCard(post: Post, isLiked: Boolean, isBookmarked: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation =  1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 10.dp,
                bottom = 10.dp
            )
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text(
                    text = if (post.isBlind) CommunityConstants.BLIND_TITLE_MESSAGE else post.title,
                    style = MaterialTheme.typography.titleMedium,
                    // 블라인드 문구는 실제 제목과 구분되도록 힌트 색으로 표시
                    color = if (post.isBlind) MaterialTheme.colorScheme.onSecondary else Color.Unspecified,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = TimeFormatter.formatTimeAgo(post.createdAt ?: 0) +
                            if (post.updatedAt != null) " (수정됨)" else "",
//                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(modifier = Modifier.offset(x = (-5).dp).padding(vertical = 3.dp)) {
                if (post.isShared ?: false) TagChip("공유")
                TagChip(post.tag.name)
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage(level = post.author.profileImg, 16)
                Text(
                    text = "  ${post.author.nickname}",
//                    fontSize = 12.sp,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                IconStat(R.drawable.ic_community_comment,
                    post.commentCount.toString(),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconStat(
                    iconRes = if (isLiked) R.drawable.ic_community_like_selected else R.drawable.ic_community_like_normal,
                    text = post.likeCount.toString(),
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconStat(
                    iconRes = if (isBookmarked) R.drawable.ic_bookmark_selected else R.drawable.ic_bookmark_normal,
                    text = post.bookmarkCount.toString(),
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun IconStat(iconRes: Any, text: String, tint: Color = Color.Black) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (iconRes) {
            is Int -> Icon(painterResource(iconRes), null, Modifier.size(14.dp), tint)
            is ImageVector -> Icon(iconRes, null, Modifier.size(14.dp), tint)
        }
        Text(
            text = " $text",
            style = MaterialTheme.typography.bodySmall,
            color = tint
        )
    }
}

// 목록 맨 아래 로딩 표시 항목의 key — 하단 도달 감지가 배치에 남아 있는지 확인하는 데도 쓴다
private const val LOADING_INDICATOR_KEY = "loading_indicator"

// 새로고침 뒤 맨 위 스크롤을 기다리는 최대 시간 — 넘으면 스크롤을 멈추고 loadMore 차단을 푼다
private const val SCROLL_TO_TOP_TIMEOUT_MS = 3_000L

@Composable
fun EmptyStateView(hasQuery: Boolean = false) {
    // 당겨서 새로고침(PullToRefreshBox)은 스크롤되는 내용에서만 당김을 받으므로, 안내 하나뿐이어도 스크롤 목록으로 둔다 (#660)
    // → 첫 불러오기 실패, 받은 글이 전부 차단한 사용자 글인데 다음 페이지 불러오기가 실패한 경우에도 당겨서 다시 시도할 수 있다
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (hasQuery) "검색어에 해당하는 게시글이 없어요" else "찾으시는 게시글이 없어요",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
