package com.a32b.plant.presentation.community.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityEvent
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityViewModel
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.TagGroup
import com.a32b.plant.presentation.core.extension.showToast

@Composable
fun CommunityActivityScreen(navController: NavController, viewModel: CommunityActivityViewModel = hiltViewModel()) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val list = listOf(ActivityType.POST, ActivityType.COMMENT, ActivityType.LIKE)

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(30.dp).align(Alignment.CenterStart)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_backbtn),
                        contentDescription = "뒤로가기"
                    )
                }
                Text("내 활동", style = MaterialTheme.typography.displayLarge)

                if (uiState.isSelectionMode) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.selectedIds.isNotEmpty()) {
                            if (uiState.isDeletingActivities) {
                                LoadingBox()
                            } else {
                                TextButton(
                                    onClick = { viewModel.deleteSelected() },
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (uiState.selected == ActivityType.LIKE)
                                            "좋아요 취소(${uiState.selectedIds.size})"
                                        else
                                            "삭제(${uiState.selectedIds.size})",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = { viewModel.toggleSelectionMode() },
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "취소",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.toggleSelectionMode() },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            painterResource(id = R.drawable.ic_edit),
                            contentDescription = "편집",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

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
                            colors = CheckboxDefaults.colors(
                                uncheckedColor = MaterialTheme.colorScheme.outline
                            )
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
