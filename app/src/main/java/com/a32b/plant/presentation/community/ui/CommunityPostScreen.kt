package com.a32b.plant.presentation.community.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.presentation.community.CommunityConstants
import com.a32b.plant.presentation.community.viewmodel.CommunityPostEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.presentation.core.component.BackTopBar
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.TagSheet
import com.a32b.plant.presentation.core.extension.showToast
import kotlinx.coroutines.flow.drop

@Composable
fun CommunityPostScreen(
    navController: NavController, viewModel: CommunityPostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val args = navController.currentBackStackEntry?.toRoute<Routes.CommunityPost>()

    val postId = args?.postId
    val potId = args?.potId
    val tag = args?.tagId //tag id
    val title = args?.title
    val studyLogIds = args?.studyLogIds

    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CommunityPostEvent.NavigateToDetail -> {
                    navController.navigate(Routes.CommunityDetail(event.postId)) {
                        if (postId != null) {
                            popUpTo<Routes.CommunityDetail> { inclusive = true }
                        } else {
                            popUpTo<Routes.CommunityPost> { inclusive = true }
                        }
                    }
                }

                is CommunityPostEvent.ShowToast -> context.showToast(event.message)
                // 종료 다이얼로그 확인과 겹쳐도 한 번만 닫히도록 CommunityPost를 대상으로 pop (이미 닫혔으면 무시됨)
                is CommunityPostEvent.NavigateBack -> navController.popBackStack<Routes.CommunityPost>(inclusive = true)
            }
        }
    }
    BackHandler {
        if (uiState.isTagSheetShown) {
            viewModel.onIsTagSheetShownChange()
        } else {
            viewModel.onIsDismissDialogShowChange()
        }
    }

    LoadableScreen(viewModel) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                PostTopBar(
                    isEditMode = postId != null,
                    isSubmitting = uiState.isSubmitting,
                    isOffline = !isOnline,
                    onBackClick = {
                        viewModel.onIsDismissDialogShowChange()
                    },
                    onRegisterClick = {
                        val isInvalid = uiState.title.isBlank() ||
                                (uiState.studyLogs == null && uiState.content.isNullOrBlank())
                        if (isInvalid) {
                            context.showToast("제목과 내용을 모두 입력해주세요.")
                        } else if (uiState.selected?.id.isNullOrEmpty()) {
                            context.showToast("태그를 선택해주세요.")
                        } else {
                            viewModel.savePost()
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    // edge-to-edge라 adjustResize만으로는 창이 줄지 않아, 키보드 높이만큼 목록 영역을 직접 줄인다 (#671).
                    // 이래야 PostInputField가 커서를 "키보드 위" 영역 안으로 스크롤할 수 있다.
                    .imePadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                item {
                    Text(
                        "제목",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PostInputField(
                        value = uiState.title,
                        onValueChange = { viewModel.onTitleChange(it) },
                        placeholder = "제목을 입력하세요",
                        singleLine = true,
                        maxLength = 50
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.isShared) {
                                viewModel.onIsTagSheetShownChange()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "태그",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (!uiState.isShared) {
                            Icon(
                                painter = painterResource(id = if (uiState.isTagSheetShown) R.drawable.ic_up else R.drawable.ic_down),
                                contentDescription = "태그박스",
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(start = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${uiState.selected?.name ?: "태그를 선택하세요"}${(if (uiState.isShared) ", 공유" else "")}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp),
                            // 공유 모드일 때는 텍스트 색상을 흐리게 하여 수정 불가임을 알림
                            color = if (uiState.isShared) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        )

                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.isTagSheetShown) {
                        Spacer(modifier = Modifier.height(2.dp))
                        TagSheet(
                            uiState.tags,
                            enable = !uiState.isShared,
                            init = when {
                                uiState.isShared || !postId.isNullOrEmpty() -> listOfNotNull(uiState.selected)
                                uiState.selected?.id?.isNotEmpty() == true -> listOfNotNull(uiState.selected)
                                else -> emptyList()
                            }
                        ) { selected ->
                            if (selected.isNotEmpty()) {
                                viewModel.onSelectedTagChange(selected[0])
                            }
                        }
                    }

                }

                if (uiState.isShared) {
                    val studyLogs = uiState.studyLogs ?: emptyList()
                    item {
                        Text("공유 기록", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        StudyLogCard(studyLogs)
                    }
                    item {
                        Text("추가 내용", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        PostInputField(
                            value = uiState.extraContent,
                            onValueChange = { viewModel.onExtraContentChange(it) },
                            placeholder = "공부 기록에 대해 자유롭게 작성해주세요. (선택)",
                            modifier = Modifier.heightIn(min = 200.dp),
                            maxLength = 1000
                        )
                    }
                } else {
                    item {
                        Text(
                            "본문",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PostInputField(
                            value = uiState.content,
                            onValueChange = { viewModel.onContentChange(it) },
                            placeholder = "나누고 싶은 이야기를 적어주세요.\n\n※ 비방이나 욕설은 제재 대상이 될 수 있습니다.",
                            modifier = Modifier.heightIn(min = 400.dp),
                            maxLength = 1000
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    if (uiState.isDismissDialogShow)
        ConfirmDialog(
            "게시글 작성을 종료하시겠습니까?",
            semiText = "작성된 게시글은 저장되지 않습니다.",
            onDismiss = { viewModel.onIsDismissDialogShowChange() },
            onConfirm = {
                // NavigateBack 이벤트와 겹쳐도 한 번만 닫히도록 동일하게 CommunityPost를 대상으로 pop
                navController.popBackStack<Routes.CommunityPost>(inclusive = true)
            }
        )
}


@Composable
fun PostTopBar(
    isEditMode: Boolean,
    isSubmitting: Boolean = false,
    // 오프라인: 누르면 ViewModel이 안내 토스트를 띄우므로 클릭은 막지 않고 alpha로만 비활성처럼 표시 (#641)
    isOffline: Boolean = false,
    onBackClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val isOfflineLooking = isOffline && !isSubmitting
    BackTopBar(
        title = if (isEditMode) "글 수정" else "글쓰기",
        onBack = onBackClick,
        actions = {
            Surface(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .alpha(if (isOfflineLooking) CommunityConstants.OFFLINE_BUTTON_ALPHA else 1f)
                    .semantics { if (isOfflineLooking) stateDescription = CommunityConstants.OFFLINE_STATE_DESCRIPTION }
                    .clickable(enabled = !isSubmitting) { onRegisterClick() },
                shape = RoundedCornerShape(4.dp),
                color = if (isSubmitting) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = if (isSubmitting) "처리 중" else if (isEditMode) "수정" else "등록",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
fun PostInputField(
    value: String?,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    maxLength: Int = Int.MAX_VALUE
) {
    val context = LocalContext.current

    // 긴 본문을 입력할 때 바깥 스크롤이 커서를 따라가도록 TextFieldState 방식을 쓴다 (#671).
    // value(String) 방식은 포커스를 얻을 때만 바깥 스크롤에 커서를 보이게 요청해, 입력 중에는 커서가 키보드에 가려졌다.
    //
    // 처음 값만 value에서 가져오고, 이후에는 사용자가 바꿀 때만 onValueChange로 알린다.
    // 입력란이 그려진 뒤 value가 바뀌어도 반영되지 않는다 (지금 호출하는 화면들은 그 뒤에 바깥에서 값을 바꾸지 않음).
    val state = remember { TextFieldState(value ?: "") }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        // 첫 방출은 처음 값 그대로라 건너뛴다 (알리면 비어 있는 공유 글 추가 내용이 null에서 ""로 바뀜)
        snapshotFlow { state.text.toString() }.drop(1).collect { currentOnValueChange(it) }
    }
    // 글자 수를 넘는 입력은 통째로 되돌리고 안내 (붙여넣기 포함, 이전 동작과 같음)
    val limitTransformation = remember(maxLength, context) {
        InputTransformation {
            if (length > maxLength) {
                revertAllChanges()
                context.showToast("${maxLength}자 이하로 입력해주세요.")
            }
        }
    }

    Column {
        TextField(
            state = state,
            inputTransformation = limitTransformation,
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            modifier = modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background),
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        Text(
            "${state.text.length} / $maxLength",
            style = MaterialTheme.typography.bodySmall,
            color = if (state.text.length >= maxLength)
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp, top = 4.dp)
        )
    }
}
