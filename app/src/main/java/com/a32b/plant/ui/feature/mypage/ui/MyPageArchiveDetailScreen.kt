package com.a32b.plant.ui.feature.mypage.ui

import android.R.attr.enabled
import android.R.attr.onClick
import android.R.attr.tag
import android.R.attr.text
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveDetailViewModel
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter.formatTimestamp
import com.a32b.plant.data.di.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale
import com.a32b.plant.core.component.ConfirmDialog

//
// 학습 완료 화분 리스트 -> 화분 상세 창
// 나무 이미지
// 시작일 : 2026년 3월 00일
// 종료일 : 2026년 3월 00일
// 총 공부 시간 152: 25: 21
// 학습 logs


@Composable
fun MyPageArchiveDetailScreen(navController: NavController) {
    val args = navController.currentBackStackEntry?.toRoute<Routes.MyPageArchiveDetail>()!!
    val potId = args.potId

    val viewModel: MyPageArchiveDetailViewModel =
        viewModel(factory = ViewModelFactory.myPageArchiveDetailViewModelFactory(potId))
    val uiState by viewModel.uiState.collectAsState()
    val pot = uiState.pot
    var isSelectionMode by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // 표준 상단바 높이
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 뒤로가기 버튼 or 선택 취소 버튼
                IconButton(onClick = {
                    if (uiState.isSelectionMode) {
                        viewModel.toggleSelectionMode(false)
                    } else {
                        navController.popBackStack()
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            id = if (uiState.isSelectionMode) R.drawable.ic_study_result_close else R.drawable.ic_backbtn
                        ),
                        contentDescription = if (uiState.isSelectionMode) "취소" else "뒤로가기",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "[${pot?.tag ?: "태그"}] ",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(onClick = {
                    if (uiState.isSelectionMode) {
                        if (uiState.selectedIds.isNotEmpty()) {
                            showDialog = true
                        } else {
                            Toast.makeText(context, "공유할 기록을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel.toggleSelectionMode(true)
                    }
                }) {
                    Icon(
                        imageVector = if (uiState.isSelectionMode) Icons.Default.Check else Icons.Default.Share,
                        contentDescription = "공유"
                    )
                }
            }
        }
    ) { paddingValues ->
        if (pot != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Scaffold 패딩 적용
                contentPadding = PaddingValues(16.dp), // 전체 여백
                verticalArrangement = Arrangement.spacedBy(12.dp) // 아이템 간 간격
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileImage(level = pot.level, size = 100)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.KOREA) }
                            Text(
                                text = "시작일 : ${pot.createdAt?.let { formatTimestamp(it) } ?: ""}"
                            )
                            Text(
                                text = "종료일 : ${pot.completedAt?.let { formatTimestamp(it) } ?: ""}"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "총 공부 시간 : ${uiState.totalStudyTime}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                // 기록 리스트
                items(uiState.logs) { log ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = isSelectionMode,
                                onClick = { viewModel.toggleSelection(log.id) }
                            ),
                        colors = CardDefaults.cardColors(

                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        ),

                        ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AnimatedVisibility(
                                visible = uiState.isSelectionMode, // isSelectionMode 가 ture 일 때 체크박스 표시
                                enter = expandHorizontally() + fadeIn(),
                                exit = shrinkHorizontally() + fadeOut()
                            ) {
                                Checkbox(
                                    checked = uiState.selectedIds.contains(log.id),
                                    onCheckedChange = { viewModel.toggleSelection(log.id) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.title} [${formatToDigitalClock(log.studyingTime)}]",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (log.contents.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    log.contents.forEach { content ->
                                        Text(
                                            text = "• $content",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // 공유 확인 다이얼로그
    if (showDialog) {
        ConfirmDialog(
            text = "커뮤니티 공유",
            semiText = "선택한 학습 기록을 커뮤니티에 공유하시겠습니까?",
            onDismiss = {
                showDialog = false
            },
            onConfirm = {
                viewModel.shareToPost { potId, tag, title, logIds ->
                    navController.navigate(
                        Routes.CommunityPost(
                            postId = null,
                            potId = potId,
                            tag = uiState.pot?.tag,
                            title = uiState.pot?.name,
                            studyLogIds = logIds
                        )
                    ) {
                        launchSingleTop = true
                    }

                    // 3. 다이얼로그 닫고 선택 모드 해제
                    showDialog = false
                    viewModel.toggleSelectionMode(false)
                }
            }
        )
    }
}