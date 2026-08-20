package com.a32b.plant.presentation.community.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.TagChip
import com.a32b.plant.presentation.core.component.TagSheet
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.domain.model.Post
import com.a32b.plant.presentation.community.viewmodel.CommunityListEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityListViewModel
import com.a32b.plant.presentation.theme.Typography
import com.a32b.plant.presentation.theme.primary

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(navController: NavController, viewModel: CommunityListViewModel = hiltViewModel()) {

    val context = LocalContext.current

    val listState = rememberLazyListState()

    // 이벤트 수신 (토스트, 맨 위로 스크롤)
    // ScrollToTop: 애니메이션 완료 후 clearSuppressLoadMore() 호출 → loadMore 차단 해제
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommunityListEvent.ShowToast -> context.showToast(event.message)
                is CommunityListEvent.ScrollToTop -> {
                    listState.animateScrollToItem(0)
                    viewModel.clearSuppressLoadMore()  // 스크롤 완료 후 loadMore 재허용
                }
            }
        }
    }

    LoadableScreen(viewModel) {
        // State<T> 참조 보존 (derivedStateOf에서 suppressLoadMore를 Compose 의존성으로 추적하기 위해)
        val uiStateFlow = viewModel.uiState.collectAsStateWithLifecycle()
        val uiState by uiStateFlow
        val filteredPosts by viewModel.filteredPosts.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

        // 하단 도달 감지 → loadMore
        // suppressLoadMore: refresh 성공 시 true → ScrollToTop 애니메이션 완료 후 false
        //   → recomposition 타이밍 레이스(47→20개로 줄어들 때 잠깐 false→true 전환) 원천 차단
        val shouldLoadMore by remember {
            derivedStateOf {
                if (uiStateFlow.value.suppressLoadMore) return@derivedStateOf false
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    ?: return@derivedStateOf false
                val total = listState.layoutInfo.totalItemsCount
                lastVisible >= total - 3
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) viewModel.loadMore()
        }

        BackHandler {
            navController.navigate(Routes.HomeMain) {
                popUpTo(Routes.HomeMain) { inclusive = false }
            }
        }

        // 필터 변경 재쿼리 중 오버레이 (LoadableScreen 이후, 이미 데이터가 있는 상태)
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
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 10.dp)
                        ) {
                            SearchBarSection(
                                query = searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) }
                            )
                            Row(modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "태그",
                                    style = Typography.titleSmall,
                                    modifier = Modifier.padding(start = 16.dp),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Icon(
                                    painter = painterResource(id = if (uiState.isTagSheetShown) R.drawable.ic_up else R.drawable.ic_down),
                                    contentDescription = "태그박스",
                                    modifier = Modifier.clickable {
                                        viewModel.onIsTagSheetShownChange()
                                    })
                                Box(modifier = Modifier.clickable {
                                    viewModel.onSelectedChanged(emptyList())
                                }) {
                                    TagChip("전체 선택 해제", 13, false)
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text("공유글 보기", style = Typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 15.sp)
                                Switch(uiState.isSharedShown, onCheckedChange = { viewModel.onSharedShownChange() },
                                    modifier = Modifier.scale(0.7f).padding(end = 10.dp))
                            }
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
                                            style = Typography.bodyMedium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            if (uiState.isTagSheetShown) {
                                TagSheet(
                                    uiState.tags,
                                    isMultiSelected = true,
                                    init = uiState.selected
                                ) { selected ->
                                    viewModel.onSelectedChanged(selected.toList())
                                    Log.d("선택된 거 ", selected.toList().toString())
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { navController.navigate(Routes.CommunityPost()) },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }

                    /**임시 위치*/
                    TextButton(onClick = {navController.navigate(Routes.CommunityActivity)}) {
                        Text("내 활동", style = Typography.titleSmall)
                    }

                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.CommunityPost()) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (postList.isEmpty()) {
                    EmptyStateView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        onClick = { navController.navigate(Routes.CommunityDetail(postId = post.postId)) }
                                    )
                                }
                                // 하단 로딩 인디케이터 (loadMore 중, 데이터 있을 때만)
                                if (uiState.hasMore && uiState.isLoadingMore && uiState.posts.isNotEmpty()) {
                                    item(key = "loading_indicator") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 필터 변경 재쿼리 로딩 오버레이
            if (uiState.hasLoadedOnce && uiState.isLoadingMore && uiState.posts.isEmpty()) {
                LoadingBox()
            }
            // 검색 전체 로드 중 오버레이
            if (uiState.isSearchLoading) {
                LoadingBox()
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
                "검색어를 입력하세요", style = Typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_community_clear),
                    contentDescription = "초기화",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            onQueryChange("")
                            focus.clearFocus()
                        },
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}

@Composable
fun PostCard(post: Post, isLiked: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = TimeFormatter.formatTimeAgo(post.createdAt ?: 0) +
                        if (post.updatedAt != null) " (수정됨)" else "",
                    fontSize = 11.sp,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                if (post.isShared ?: false) TagChip("공유", 10)
                TagChip(post.tag.name, 10)
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage(level = post.author.profileImg, 16)
                Text(
                    text = "  ${post.author.nickname}",
                    fontSize = 12.sp,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                IconStat(R.drawable.ic_community_comment, post.commentCount.toString())
                Spacer(modifier = Modifier.width(12.dp))
                IconStat(
                    iconRes = if (isLiked) R.drawable.ic_community_like_selected else R.drawable.ic_community_like_normal,
                    text = post.likeCount.toString(),
                    tint = if (isLiked) primary else Color.Black
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
        Text(text = " $text", fontSize = 11.sp, color = tint)
    }
}

@Composable
fun EmptyStateView(hasQuery: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasQuery) "검색어에 해당하는 게시글이 없어요 😭" else "찾으시는 게시글이 없어요 😭",
            color = Color.Black
        )
    }
}
