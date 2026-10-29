package com.a32b.plant.presentation.community.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.flow.first
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.type.ReportReason
import com.a32b.plant.presentation.community.CommunityConstants
import com.a32b.plant.presentation.community.viewmodel.CommunityDetailEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.TagChip
import com.a32b.plant.presentation.core.extension.showToast
import kotlinx.coroutines.launch

// 댓글 작성/수정 입력란 공통 최대 줄 수 - 편집 영역이 키보드 위 공간보다 커지면 bringIntoView로
// 등록·저장 버튼까지 보이게 할 수 없어 제한한다(#417). 넘으면 입력란 안에서 스크롤된다.
private const val COMMENT_MAX_LINES = 4

// LazyColumn에서 게시글 헤더/태그/본문을 하나의 item으로 합친 뒤의 댓글 입력창 item 인덱스
// (헤더+태그+본문 item, 구분선+아이콘 item, 댓글 입력창 item 순으로 고정)
private const val COMMENT_INPUT_ITEM_INDEX = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityDetailScreen(
    navController: NavController, viewModel: CommunityDetailViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val postState by viewModel.post.collectAsStateWithLifecycle()
    val showDeleteDialog = viewModel.showDeleteDialog.value


    val isLikedByMe = postState?.isLiked == true
    val isBookmarkedByMe = postState?.isBookmarked == true

    val listState = rememberLazyListState()
    var isCommentFieldFocused by remember { mutableStateOf(false) }
    val commentInputRequester = remember { BringIntoViewRequester() }

    // 댓글 입력창 포커스 + 키보드 노출 시, 입력창이 리스트 최상단으로 오도록 스크롤해
    // 입력창~키보드 사이에 기존 댓글들이 자연스럽게 보이게 함 (일반적인 커뮤니티 앱 동작과 동일)
    //
    // 이전에는 WindowInsets.ime 높이를 화면 최상단에서 읽어 키보드 애니메이션 프레임마다
    // 화면 전체가 recompose되고 LaunchedEffect가 취소·재시작되며 scrollToItem을 반복 호출해
    // 버벅였다(#412). 이제 ime 객체만 잡아두고 높이는 effect 안(snapshotFlow)에서만 읽으며,
    // "키보드가 열려 있는지(열리는 중 포함)"만 derivedStateOf로 뽑아 key로 써서 키보드가 열리고 닫힐 때만
    // recompose/재시작되게 한다. 이모지 키보드 전환처럼 열린 상태에서 높이만 바뀌는 경우는
    // key가 바뀌지 않아 사용자가 스크롤해둔 위치를 되돌리지 않는다.
    //
    // 스크롤은 키보드가 열릴 때 (a)(b) 최대 2번, 이후에는 (c) 입력 카드 높이가 커질 때만 한다:
    // (a) 열리기 시작할 때 scrollToItem 1번 - 입력창이 리스트 최상단에 오면 LazyColumn은 상단 기준으로
    //     위치를 유지하므로, 이후 imePadding으로 viewport가 줄어들어도 매 프레임 보정할 필요가 없다.
    // (b) 키보드 애니메이션이 끝난 뒤 입력 카드 bringIntoView 1번 - 댓글이 적어 입력창을 최상단까지
    //     못 올린 경우, LazyColumn 내장 "포커스 입력란 유지" 스크롤은 TextField까지만 보이게 해
    //     아래 등록 버튼·글자수가 가려질 수 있어 카드 전체를 마무리 보정한다. 이미 보이면 아무 동작도 하지 않음.
    //
    // (c) 키보드가 열린 동안 여러 줄 입력으로 입력 카드 높이가 커질 때마다 bringIntoView 1번(#417) -
    //     타이핑 중 내장 스크롤은 커서까지만 보이게 해 아래 등록 버튼·글자수가 가려질 수 있다.
    //     타이핑마다 호출하면 버벅이므로(#380) 높이가 커질 때만 보정한다. 한 줄 안 타이핑·maxLines 도달 후
    //     입력은 높이가 그대로라 동작하지 않고, 줄어들 때는 리스트가 튀지 않게 보정하지 않는다.
    //     높이는 onSizeChanged로 state에만 기록하고 effect 안(snapshotFlow)에서만 읽어 recompose되지 않게 한다.
    val density = LocalDensity.current
    var commentInputHeightPx by remember { mutableIntStateOf(0) }
    val imeInsets = WindowInsets.ime
    val imeTargetInsets = WindowInsets.imeAnimationTarget
    val isImeVisible by remember(density, imeTargetInsets) {
        derivedStateOf { imeTargetInsets.getBottom(density) > 0 }
    }
    LaunchedEffect(isCommentFieldFocused, isImeVisible) {
        if (!isCommentFieldFocused || !isImeVisible) return@LaunchedEffect

        // (a) 자식 코루틴으로 실행해, 내장 스크롤에 선점돼 취소(MutationInterruptedException)돼도 (b)까지 막히지 않게 한다.
        launch { listState.scrollToItem(COMMENT_INPUT_ITEM_INDEX) }

        // 키보드 애니메이션 종료 대기 + 줄어든 viewport가 레이아웃에 반영될 한 프레임 대기
        snapshotFlow { imeInsets.getBottom(density) >= imeTargetInsets.getBottom(density) }.first { it }
        withFrameNanos { }

        // (b) bringIntoView()가 CancellationException 외의 예외(예: requester에 연결된 노드가 없는 경우의
        // IllegalStateException)를 던지면 LaunchedEffect가 죽을 수 있어 safeRunCatching으로 흡수한다.
        safeRunCatching { commentInputRequester.bringIntoView() }

        // (c) 이후 키보드가 열려 있는 동안 입력 카드 높이가 커질 때만 보정 (키보드가 닫히거나 포커스가 빠지면 key 변경으로 취소)
        commentInputRequester.bringIntoViewOnHeightGrow { commentInputHeightPx }
    }

    // 댓글 수정 중 뒤로가기 시 화면을 나가지 않고 수정만 취소
    // (키보드가 열려 있으면 첫 번째 뒤로가기는 시스템이 키보드를 닫는 데 사용)
    // 저장 요청 중에는 비활성화해 평소처럼 화면을 나가게 함
    val editingCommentId = uiState.editingCommentId
    BackHandler(
        enabled = editingCommentId != null && editingCommentId !in uiState.editingSubmittingCommentIds
    ) {
        viewModel.cancelEditComment()
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CommunityDetailEvent.ShowToast -> context.showToast(event.message)
                is CommunityDetailEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    // 게시글 삭제 다이얼로그 (core/component/ConfirmDialog.kt - 다이얼로그 공통소스 ConfirmDialog로 변경)
    if (showDeleteDialog) {
        ConfirmDialog(
            text = "게시글을 삭제하시겠습니까?",
            onDismiss = { viewModel.closeDeleteDialog() },
            onConfirm = { viewModel.deletePost() }
        )
    }

    // 댓글 삭제 다이얼로그
    if (uiState.deletingCommentId != null) {
        ConfirmDialog(
            text = "댓글을 삭제하시겠습니까?",
            onDismiss = { viewModel.closeCommentDeleteDialog() },
            onConfirm = { viewModel.deleteComment() }
        )
    }

    // 신고 BottomSheet
    if (uiState.showReportSheet) {
        ReportBottomSheet(
            selectedReason = uiState.selectedReportReason,
            detailText = uiState.reportDetailText,
            isSubmitting = uiState.isReportSubmitting,
            onReasonSelect = { viewModel.selectReportReason(it) },
            onDetailChange = { viewModel.onReportDetailChange(it) },
            onDismiss = { viewModel.closeReportSheet() },
            onConfirm = { viewModel.submitReport() }
        )
    }

    LoadableScreen(viewModel) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                if (postState == null) {
                    // 삭제된 게시글 / 조회 실패 — 빈 화면에서는 뒤로가기·당겨서 새로고침이 불가능하므로 안내 UI 표시
                    if (uiState.isPostNotFound || uiState.isLoadFailed) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.isPostNotFound) "삭제된 게시글입니다." else "게시글을 불러오지 못했습니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row {
                                    if (!uiState.isPostNotFound) {
                                        // 새로고침 중 연타로 refresh()가 중복 실행되지 않도록 비활성화
                                        TextButton(
                                            onClick = { viewModel.refresh() },
                                            enabled = !uiState.isRefreshing
                                        ) {
                                            Text("다시 시도")
                                        }
                                    }
                                    TextButton(onClick = { navController.popBackStack() }) {
                                        Text("돌아가기")
                                    }
                                }
                            }
                        }
                    }
                    return@Scaffold
                }
                val currentPost = postState!!

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .imePadding()
                        .padding(horizontal = 10.dp)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.elevatedCardElevation(1.dp)
                    ) {
                        val isPostBlind = currentPost.isBlind

                        // 댓글 입력창에 포커스가 가면 listState.scrollToItem(COMMENT_INPUT_ITEM_INDEX)로
                        // 스크롤하므로, 헤더+태그+본문을 하나의 item으로 합쳐 댓글 입력창의 인덱스를
                        // isPostBlind/studyLogs 유무와 무관하게 고정값(0: 헤더, 1: 구분선+아이콘, 2: 댓글 입력창)으로 유지한다.
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
                        ) {

                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { navController.popBackStack() },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .align(Alignment.CenterStart)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_backbtn),
                                            contentDescription = "뒤로가기",
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                                        )
                                    }
                                    Text(
                                        if (isPostBlind) CommunityConstants.BLIND_TITLE_MESSAGE else currentPost.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        // 블라인드 문구는 실제 제목과 구분되도록 힌트 색으로 표시
                                        color = if (isPostBlind) MaterialTheme.colorScheme.onSecondary else Color.Unspecified,
                                        modifier = Modifier.padding(horizontal = 46.dp),
                                        textAlign = TextAlign.Center
                                    )

                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ProfileImage(currentPost.author.profileImg, 24)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        currentPost.author.nickname,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        TimeFormatter.formatTimeWithClock(currentPost.createdAt ?: 0) +
                                                if (currentPost.updatedAt != null) " (수정됨)" else "",
                                        style = MaterialTheme.typography.labelMedium,
    //                                fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.offset(x = (-3).dp)) {
                                    if (currentPost.isShared ?: false) TagChip("공유", 12)

                                    TagChip(currentPost.tag.name, 12)
                                }
                                Spacer(modifier = Modifier.height(20.dp))

                                if (isPostBlind) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            CommunityConstants.BLIND_CONTENT_MESSAGE,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondary
                                        )
                                    }
                                } else {
                                    if (!currentPost.studyLogs.isNullOrEmpty()) {
                                        StudyLogCard(currentPost.studyLogs)
                                    }
                                    if (!currentPost.studyLogs.isNullOrEmpty() && !currentPost.content.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    Text(
                                        currentPost.content ?: "",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(28.dp))

                                HorizontalDivider(
                                    color =
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
    //                            sub3.copy(alpha = 0.5f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Icon(
                                        painterResource(id = R.drawable.ic_community_comment), null,
                                        tint = MaterialTheme.colorScheme.onSurface,
    //                                tint = Color.Gray
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        " ${currentPost.commentCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(end = 16.dp)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.toggleLike() }
                                    ) {
                                        Icon(
                                            painterResource(id = if (isLikedByMe) R.drawable.ic_community_like_selected else R.drawable.ic_community_like_normal),
                                            tint = if (isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp),
                                            contentDescription = null
                                        )
                                        Text(
                                            " ${currentPost.likeCount}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewModel.toggleBookmark() }
                                    ) {
                                        Icon(
                                            painterResource(id = if (isBookmarkedByMe) R.drawable.ic_bookmark_selected else R.drawable.ic_bookmark_normal),
                                            tint = if (isBookmarkedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp),
                                            contentDescription = null
                                        )
                                        Text(
                                            " ${currentPost.bookmarkCount}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    if (currentPost.author.id == uiState.currentUid) {
                                        // 블라인드된 게시글은 수정 화면에서 원래 제목/본문이 다시 보이므로 수정 진입 차단 (삭제는 허용)
                                        if (!isPostBlind) {
                                            IconButton(onClick = {
                                                navController.navigate(Routes.CommunityPost(postId = currentPost.postId))
                                            }) {
                                                Icon(
                                                    painterResource(id = R.drawable.ic_edit), null,
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(onClick = { viewModel.openDeleteDialog() }) {
                                            Icon(
                                                painterResource(id = R.drawable.ic_trash), null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else if (!isPostBlind) {
                                        IconButton(onClick = { viewModel.openPostReportSheet() }) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_siren_report),
                                                contentDescription = "신고",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }


                            item {
                                CommentInputSection(
                                    nickname = uiState.currentNickname,
                                    profileImg = uiState.currentProfileImg,
                                    text = uiState.comment,
                                    // 블라인드 게시글은 새 댓글 차단 — item 자체는 남겨 COMMENT_INPUT_ITEM_INDEX를 고정값으로 유지
                                    isBlocked = isPostBlind,
                                    onTextChange = { viewModel.onCommentChange(it) },
                                    onSend = { viewModel.addComment() },
                                    onBlockedClick = { viewModel.onBlockedCommentInputClick() },
                                    onFocusChanged = { isCommentFieldFocused = it },
                                    modifier = Modifier
                                        .bringIntoViewRequester(commentInputRequester)
                                        .onSizeChanged { commentInputHeightPx = it.height }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // CommentRow에 수정/삭제 기능 파라미터 전달
                            items(uiState.commentList) { commentData ->
                                CommentRow(
                                    comment = commentData,
                                    isOwner = commentData.user.uid == uiState.currentUid,
                                    // 블라인드 게시글의 댓글은 수정 진입 차단 (삭제는 허용)
                                    canEdit = !isPostBlind,
                                    isEditing = uiState.editingCommentId == commentData.commentId,
                                    editingText = uiState.editingCommentText,
                                    isEditSubmitting = commentData.commentId in uiState.editingSubmittingCommentIds,
                                    onEditTextChange = { viewModel.onEditCommentTextChange(it) },
                                    onEditStart = { viewModel.startEditComment(commentData) },
                                    onEditSubmit = { viewModel.submitEditComment() },
                                    onEditCancel = { viewModel.cancelEditComment() },
                                    onDeleteClick = { viewModel.openCommentDeleteDialog(commentData.commentId) },
                                    onReportClick = { viewModel.openCommentReportSheet(commentData.commentId) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// CommentRow에 인라인 편집 모드 + 수정/삭제 아이콘 추가
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommentRow(
    comment: Comment,
    isOwner: Boolean,
    canEdit: Boolean = true,
    isEditing: Boolean,
    editingText: String,
    isEditSubmitting: Boolean = false,
    onEditTextChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onEditSubmit: () -> Unit,
    onEditCancel: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val maxLength = 100
    var isEditFieldFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (!isEditing) isEditFieldFocused = false
    }

    // 키보드가 올라와 포커스된 입력란이 가려지면 LazyColumn에 내장된 "포커스 입력란 유지" 스크롤이
    // 자동으로 개입한다. LazyList 스크롤은 MutatorMutex로 한 번에 하나만 실행되므로, 직접
    // listState.animateScrollBy()로 스크롤하면 이 내장 스크롤에 선점돼 취소된다(아래쪽 댓글처럼
    // 입력란 자체가 가려지는 경우에만 재현). 내장 스크롤은 입력란(커서)만 보이게 할 뿐 그 아래
    // 저장/취소 버튼은 신경 쓰지 않아 버튼이 가려진 채로 남는다.
    //
    // bringIntoView()는 내장 스크롤과 같은 처리기가 받아 함께 계산하므로 서로 선점하지 않는다.
    // TextField+저장/취소 Row 전체를 요청 영역으로 잡아 버튼까지 보이게 한다. 매 프레임 호출하면
    // 버벅이므로(#380), 포커스당 (a)(b) 최대 2번, 이후에는 (c) 편집 영역 높이가 커질 때만 호출한다:
    // (a) 키보드가 열리기 시작할 때 1번,
    // (b) 키보드 애니메이션이 끝난 뒤(위쪽 댓글처럼 (a) 시점엔 보였지만 viewport가 줄며 가려진
    //     경우를 마무리 보정) 1번,
    // (c) 여러 줄 입력으로 편집 영역 높이가 커질 때마다 1번(#417) - 타이핑 중 내장 스크롤은
    //     커서까지만 보이게 해 저장/취소 버튼이 가려질 수 있어서다. TextField는 maxLines(4줄)로 제한해
    //     편집 영역이 화면보다 커지지 않게 한다(화면보다 큰 영역은 bringIntoView로 버튼까지 보이게 할 수 없음).
    //     4줄 도달 후 입력은 높이가 그대로라 동작하지 않는다.
    //     높이는 onSizeChanged로 state에만 기록하고 effect 안에서만 읽어 recompose되지 않게 한다.
    val editAreaRequester = remember { BringIntoViewRequester() }
    var editAreaHeightPx by remember { mutableIntStateOf(0) }
    if (isEditing) {
        val density = LocalDensity.current
        // ime 객체만 잡아두고 실제 높이는 effect 안에서 읽어, 키보드 애니메이션 프레임마다
        // recompose되지 않게 한다.
        val imeInsets = WindowInsets.ime
        val imeTargetPx = WindowInsets.imeAnimationTarget.getBottom(density)

        LaunchedEffect(isEditFieldFocused, imeTargetPx) {
            if (!isEditFieldFocused || imeTargetPx <= 0) return@LaunchedEffect

            // (a) 시작 시 1회 - 자식 코루틴으로 실행해, 내장 스크롤에 선점돼 취소돼도 (b)까지 막히지 않게 한다.
            // bringIntoView()가 CancellationException 외의 예외(예: requester에 연결된 노드가 없는 경우의
            // IllegalStateException)를 던지면 launch의 부모(이 LaunchedEffect)까지 전파돼 죽을 수 있어
            // safeRunCatching으로 흡수한다(CancellationException은 재전파, 그 외는 Throwable까지 무시).
            launch { safeRunCatching { editAreaRequester.bringIntoView() } }

            // 키보드 애니메이션 종료 대기 + 줄어든 viewport가 레이아웃에 반영될 한 프레임 대기
            snapshotFlow { imeInsets.getBottom(density) }.first { it >= imeTargetPx }
            withFrameNanos { }

            // (b) 종료 후 1회 - 이미 보이면 아무 동작도 하지 않음
            safeRunCatching { editAreaRequester.bringIntoView() }

            // (c) 이후 편집 영역 높이가 커질 때만 보정 - 한 줄 안 타이핑이나 줄 삭제 시에는 동작하지 않음
            editAreaRequester.bringIntoViewOnHeightGrow { editAreaHeightPx }
        }
    }

    // 서버 반영 전 임시 댓글은 실제 commentId가 없어 수정/삭제/신고 아이콘을 숨기고 흐리게 표시
    val isPending = comment.isPending

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.alpha(if (isPending) 0.5f else 1f)
    ) {
        ProfileImage(comment.user.profileImg, 24)
        Spacer(Modifier.width(5.dp))
        Column(modifier = Modifier.weight(1f)) {
                val isCommentBlind = comment.isBlind
                // 닉네임 + 작성 시간을 한 줄에 표시
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.user.nickname,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPending) "등록 중" else comment.createdAt?.let { TimeFormatter.formatTimeWithClock(it) } ?: "",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (!isOwner && !isEditing && !isCommentBlind && !isPending) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_siren_report),
                            contentDescription = "신고",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable { onReportClick() }
                                .padding(8.dp)
                                .size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                if (isCommentBlind) {
                    Text(
                        CommunityConstants.BLIND_CONTENT_MESSAGE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondary
                    )
                } else if (isEditing) {
                    // 인라인 편집 모드 - TextField + 저장/취소 Row를 하나의 영역으로 묶어
                    // bringIntoView() 요청 시 버튼까지 함께 화면에 보이게 한다.
                    Column(
                        modifier = Modifier
                            .bringIntoViewRequester(editAreaRequester)
                            .onSizeChanged { editAreaHeightPx = it.height }
                    ) {
                        TextField(
                            value = editingText,
                            onValueChange = { input ->
                                limitLength(input, maxLength, context, onEditTextChange)
                            },
                            readOnly = isEditSubmitting,
                            maxLines = COMMENT_MAX_LINES,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isEditFieldFocused = it.isFocused },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)

                        )
                        // TextButton 최소 터치 영역(48dp)으로 Row가 높아지므로 세로 가운데 정렬해
                        // 글자수 표시가 TextField에 붙지 않게 한다.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // labelMedium에 지정된 색(onSecondary)이 TextButton의 contentColor보다 우선하므로
                            // 아래 contentColor 분기는 글자 색에 반영되지 않는다. 글자 색은 Text의 color로 지정
                            // (ripple 색에는 반영되므로 분기는 유지)
                            TextButton(
                                onClick = onEditSubmit,
                                enabled = !isEditSubmitting,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (editingText.isBlank())
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    if (isEditSubmitting) "저장 중" else "저장",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(onClick = onEditCancel, enabled = !isEditSubmitting) {
                                Text(
                                    "취소",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                "${editingText.length} / $maxLength",
                                color = if (editingText.length >= maxLength)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 20.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        comment.content,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

        // 본인 댓글이고 편집 중이 아니며 서버에 반영된 댓글일 때만 수정/삭제 아이콘 표시
        if (isOwner && !isEditing && !isPending) {
            if (canEdit) {
                IconButton(onClick = onEditStart, modifier = Modifier.size(28.dp)) {
                    Icon(
                        painterResource(id = R.drawable.ic_edit), null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    painterResource(id = R.drawable.ic_trash), null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CommentInputSection(
    nickname: String,
    profileImg: String,
    text: String,
    modifier: Modifier = Modifier,
    // true면 비활성처럼 보이게 하고 탭·전송 시 onBlockedClick만 호출 (블라인드 게시글)
    isBlocked: Boolean = false,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onBlockedClick: () -> Unit = {},
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val maxLength = 100
    val focus = LocalFocusManager.current

    // 입력 중에 블라인드로 바뀌면(refresh 반영) 포커스를 빼 키보드를 닫음 — 입력한 텍스트는 유지
    LaunchedEffect(isBlocked) {
        if (isBlocked) focus.clearFocus()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isBlocked) 0.5f else 1f),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        // enabled = false는 onClick 자체를 막아 Toast를 띄울 수 없으므로, 차단 시에는 alpha로 비활성처럼 보이게 하고
        // 위에 덮은 투명 레이어가 탭을 받아 TextField 포커스(키보드)·등록 버튼 대신 onBlockedClick을 호출한다
        Box {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 12.dp)
                    // 차단 시 TalkBack이 아래 TextField로 바로 가면 Toast 없이 포커스만 잡히므로, 안내는 위 레이어 하나로만 읽히게 함
                    .then(if (isBlocked) Modifier.clearAndSetSemantics { } else Modifier)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileImage(level = profileImg, size = 24)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        nickname,
    //                fontSize = 13.sp,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                TextField(
                    value = text,
                    onValueChange = { input ->
                        limitLength(input, maxLength, context, onTextChange)
                    },
                    // 레이어가 탭을 막지만, 접근성 등 다른 경로로 포커스가 와도 키보드가 뜨지 않도록 readOnly 유지
                    readOnly = isBlocked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    placeholder = {
                        Text(
                            if (isBlocked) CommunityConstants.BLIND_POST_COMMENT_BLOCKED_MESSAGE else "댓글을 남겨보세요...",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = COMMENT_MAX_LINES,
                    minLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Default
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${text.length} / $maxLength",
                        modifier = Modifier.padding(start = 4.dp),
                        color = if (text.length >= maxLength) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            onSend()
                            // blank 제출 시에는 Toast 안내만 띄우고 포커스는 유지해 바로 이어서 입력 가능하게 함
                            if (text.isNotBlank()) focus.clearFocus()
                        },
                        // 낙관적 UI로 등록 즉시 입력창이 비워지므로 등록 중 비활성 상태가 없음
                        modifier = Modifier
                            .height(28.dp)
                            .padding(top = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            // 차단 시에는 입력해 둔 텍스트가 남아 있어도 비활성 색으로 표시 (활성 버튼처럼 보이지 않게)
                            containerColor = if (text.isBlank() || isBlocked)
                                MaterialTheme.colorScheme.tertiaryContainer
                            else
                                MaterialTheme.colorScheme.primary,
                            contentColor = if (text.isBlank() || isBlocked)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "등록",
    //                        fontSize = 12.sp,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .semantics { contentDescription = CommunityConstants.BLIND_POST_COMMENT_BLOCKED_MESSAGE }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBlockedClick
                        )
                )
            }
        }
    }
}

// heightPx()가 이전보다 커질 때만 bringIntoView()를 1번 호출한다(#417).
// 높이는 반드시 람다로 받아 snapshotFlow 안에서만 읽어야 변화를 감지하면서도 recompose되지 않는다.
// 같은 높이(한 줄 안 타이핑, maxLines 도달 후 입력)나 줄어들 때는 호출하지 않는다.
// 호출한 LaunchedEffect가 취소될 때까지 계속 감지한다. 키보드가 열려 있을 때만 쓰려면
// 호출하는 쪽 effect의 key(키보드 노출·포커스 여부)로 제한해야 한다.
private suspend fun BringIntoViewRequester.bringIntoViewOnHeightGrow(heightPx: () -> Int) {
    var prevHeightPx = heightPx()
    snapshotFlow { heightPx() }.collect { currentPx ->
        // bringIntoView()의 CancellationException 외 예외로 호출한 effect가 죽지 않게 safeRunCatching으로 흡수
        if (currentPx > prevHeightPx) safeRunCatching { bringIntoView() }
        prevHeightPx = currentPx
    }
}

fun limitLength(input: String, maxLength: Int, context: Context, onValueChange: (String) -> Unit) {
    if (input.length <= maxLength) {
        onValueChange(input)
    } else {
        Toast.makeText(context, "${maxLength}자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    selectedReason: ReportReason?,
    detailText: String,
    isSubmitting: Boolean,
    onReasonSelect: (ReportReason) -> Unit,
    onDetailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "신고 사유 선택",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ReportReason.entries.forEach { reason ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedReason == reason,
                            onClick = { onReasonSelect(reason) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedReason == reason,
                        onClick = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(reason.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (selectedReason == ReportReason.OTHER) {
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = detailText,
                    onValueChange = { if (it.length <= 200) onDetailChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            if (it.isFocused) {
                                scope.launch { sheetState.expand() }
                            }
                        },
                    placeholder = {
                        Text("상세 내용을 입력해주세요.", style = MaterialTheme.typography.labelMedium)
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onConfirm,
                enabled = selectedReason != null && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (isSubmitting) "신고 중..." else "신고하기",
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
