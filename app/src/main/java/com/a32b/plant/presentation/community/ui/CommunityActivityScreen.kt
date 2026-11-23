package com.a32b.plant.presentation.community.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityViewModel
import com.a32b.plant.presentation.core.component.BackTopBar
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.plantCheckboxColors
import com.a32b.plant.presentation.core.component.TagGroup
import com.a32b.plant.presentation.core.extension.showToast

@Composable
fun CommunityActivityScreen(navController: NavController, viewModel: CommunityActivityViewModel = hiltViewModel()) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val list = listOf(ActivityType.POST, ActivityType.COMMENT, ActivityType.LIKE, ActivityType.BOOKMARK)

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CommunityActivityEvent.NavigateToCommunityDetail -> {
                    navController.navigate(Routes.CommunityDetail(event.postId))
                }
                is CommunityActivityEvent.ShowToast -> {
                    context.showToast(event.message)
                }
            }
        }
    }

    // 선택 모드면 시스템 뒤로가기도 상단바 "취소"처럼 모드만 해제한다 (#748)
    // 삭제 중(isLoading)에는 가로채지 않아 지금처럼 화면을 나갈 수 있다. 삭제는 NonCancellable로 끝까지 진행 (#712)
    BackHandler(enabled = uiState.isSelectionMode && !uiState.isLoading) {
        viewModel.exitSelectionMode()
    }

    if (uiState.isDeleteDialogShown) {
        val (text, semiText) = when (uiState.selected) {
            ActivityType.LIKE -> "선택한 좋아요를 취소하시겠습니까?" to null
            ActivityType.BOOKMARK -> "선택한 북마크를 취소하시겠습니까?" to null
            else -> "선택한 항목을 삭제하시겠습니까?" to "삭제된 데이터는 복구할 수 없습니다."
        }
        ConfirmDialog(
            text = text,
            semiText = semiText,
            onDismiss = { viewModel.hideDeleteDialog() },
            onConfirm = {
                viewModel.hideDeleteDialog()
                viewModel.deleteSelected()
            },
            textStyle = MaterialTheme.typography.titleMedium,
            buttonTextStyle = MaterialTheme.typography.labelLarge
        )
    }

    LoadableScreen(viewModel) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                BackTopBar(
                    title = "내 활동",
                    onBack = { navController.popBackStack() }
                ) {
                    if (uiState.isSelectionMode) {
                        if (uiState.selectedCount > 0) {
                            // 좁은 화면·큰 글꼴에서 BackTopBar가 actions 폭을 제한하면 이 버튼이 남은 폭만 쓰고,
                            // 문구만 말줄임해 선택 개수 "(n)"은 항상 보이게 한다 (#645)
                            TextButton(
                                onClick = { viewModel.showDeleteDialog() },
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    when (uiState.selected) {
                                        ActivityType.LIKE -> "좋아요 취소"
                                        ActivityType.BOOKMARK -> "북마크 취소"
                                        else -> "삭제"
                                    },
                                    modifier = Modifier.weight(1f, fill = false),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "(${uiState.selectedCount})",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
                                )
                            }
                        }
                        TextButton(
                            onClick = { viewModel.exitSelectionMode() }
                        ) {
                            Text(
                                "취소",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    } else {
                        TextButton(
                            onClick = { viewModel.toggleSelectionMode() }
                        ) {
                            Text(
                                "선택",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 상단바는 화면 끝까지, 아래 내용은 이전처럼 좌우·아래 15dp 여백
                Column(modifier = Modifier.fillMaxSize().padding(start = 15.dp, end = 15.dp, bottom = 15.dp)) {
                    TagGroup(list, init = listOf(uiState.selected), isMultiSelected = false) { selected ->
                        viewModel.onSelectedChange(selected.get(0))
                    }

                    if (uiState.isLoading) {
                        LoadingBox()
                    } else if (uiState.activities.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "커뮤니티 활동 내역이 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    } else {
                        ContentList(
                            lists = uiState.activities,
                            isSelectionMode = uiState.isSelectionMode,
                            selectedIds = uiState.selectedIds,
                            onToggleSelection = { id -> viewModel.toggleItemSelection(id) },
                            onClick = { targetId ->
                                viewModel.moveToCommunityDetail(targetId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContentList(
    lists: List<CommunityActivity>,
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onToggleSelection: (String) -> Unit = {},
    onClick: (String) -> Unit
) {
    LazyColumn {
        items(lists) { list ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                shape = RoundedCornerShape(7.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                onClick = {
                    if (isSelectionMode) onToggleSelection(list.id)
                    else onClick(list.targetId)
                }
            ) {
                Row(
                    modifier = Modifier.padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = list.id in selectedIds,
                            onCheckedChange = { onToggleSelection(list.id) },
                            modifier = Modifier.size(24.dp),
                            colors = plantCheckboxColors()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(30.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                list.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                TimeFormatter.formatTimeToDate(list.createAt ?: 0),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        list.comment?.let {
                            Text(
                                list.comment,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
