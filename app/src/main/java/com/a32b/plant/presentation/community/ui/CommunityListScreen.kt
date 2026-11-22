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
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // ScrollToTop: 애니메이션 완료 후 clearSuppressLoadMore() 호출 → loadMore 차단 해제
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityListEvent.ShowToast -> context.showToast(event.message)
                is CommunityListEvent.ScrollToTop -> {
                    try {
                        listState.animateScrollToItem(0)
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
        // 하단 근처면 지금까지 받은 글 수(uiState.posts, 차단한 사용자 글 포함), 아니면 -1을 보낸다.
        // 받은 글 수가 바뀌면(페이지 추가·글 삭제·새로고침 등) 다시 보내므로, 받은 페이지 일부가 차단한 사용자 글이라(#660) 목록이 화면을 채우지 못해
        // 계속 하단 근처에 있어도 다음 페이지를 이어 부른다.
        // 화면 항목 수(totalItemsCount)를 쓰지 않는 이유: 로딩 표시 항목이 생겼다 사라지며 값이 바뀌어, 불러오기가 실패하면 곧바로 다시 부르기를 반복한다
        // 검색 중에는 보내지 않는다: 검색 결과는 전체 로드(searchResults)라 페이지가 필요 없고,
        // 결과가 짧아 계속 하단 근처면 받은 글 수가 바뀔 때마다 다음 페이지를 불러 전체를 페이지 단위로 다 받게 된다
        // 새로고침 뒤 맨 위 스크롤이 끝날 때까지(suppressLoadMore)도 -1을 보낸다: 막힌 동안 글 수를 보내 버리면
        // 막힘이 풀린 뒤 같은 값이라 다시 보내지 않아, 짧은 목록에서 다음 페이지를 부르지 않게 된다
        LaunchedEffect(listState) {
            snapshotFlow {
                if (searchQuery.isNotBlank() || uiStateFlow.value.suppressLoadMore) return@snapshotFlow -1
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: return@snapshotFlow -1
                val total = listState.layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - 3) uiStateFlow.value.posts.size else -1
            }
                .distinctUntilChanged()
                .collect { nearBottomTotal ->
                    if (nearBottomTotal >= 0 && !uiStateFlow.value.suppressLoadMore) {
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
                                    onClick = { navController.navigate(Routes.CommunityActivity) }
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(start = 16.dp)
                                            .clickable { viewModel.onIsTagSheetShownChange() }
                                    ) {
                                        Text(
                                            "태그",
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                        Icon(
                                            painter = painterResource(id = if (uiState.isTagSheetShown) R.drawable.ic_up else R.drawable.ic_down),
                                            contentDescription = "태그박스",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Box(modifier = Modifier.clickable {
                                        viewModel.onSelectedChanged(emptyList())
                                    }) {
                                        TagChip("전체 선택 해제", 13, false)
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        "공유글 보기",
                                        style =
                                            MaterialTheme.typography.titleMedium,
                                            fontSize = 14.sp,
                                    )
                                    Switch(
                                        uiState.isSharedShown,
                                        onCheckedChange = { viewModel.onSharedShownChange() },
                                        modifier = Modifier
                                            .scale(0.7f)
                                            .padding(end = 10.dp),
                                        colors = SwitchDefaults.colors(
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            uncheckedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
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
                                                onClick = {
                                                    navController.navigate(
                                                        Routes.CommunityDetail(
                                                            postId = post.postId
                                                        )
                                                    )
                                                }
                                            )
                                        }
                                        // 하단 로딩 인디케이터 (loadMore 중, 데이터 있을 때만)
                                        if (uiState.hasMore && uiState.isLoadingMore && uiState.posts.isNotEmpty()) {
                                            item(key = "loading_indicator") {
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
                            onClick = { navController.navigate(Routes.CommunityPost()) },
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


@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "검색어를 입력하세요",
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
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
                if (post.isShared ?: false) TagChip("공유", 12)
                TagChip(post.tag.name, 12)
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

@Composable
fun EmptyStateView(hasQuery: Boolean = false) {
    // 당겨서 새로고침(PullToRefreshBox)은 스크롤되는 내용에서만 당김을 받으므로, 안내 하나뿐이어도 스크롤 목록으로 둔다 (#660)
    // → 첫 불러오기 실패, 받은 글이 전부 차단한 사용자 글인데 다음 페이지 불러오기가 실패한 경우에도 당겨서 다시 시도할 수 있다
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (hasQuery) "검색어에 해당하는 게시글이 없어요 😭" else "찾으시는 게시글이 없어요 😭",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
