package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import java.time.ZoneId

//
// 학습 완료 화분 리스트 -> 화분 상세 창
// 나무 이미지
// 시작일 : 2026년 3월 00일
// 종료일 : 2026년 3월 00일
// 총 공부 시간 152: 25: 21
// 학습 logs

@OptIn(ExperimentalMaterial3Api::class)
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
        // ---------- 일반 모드 : 뒤로가기, [태그] 완료된 학습화분 이름, 공유하기 버튼
        // ---------- 공유 모드 : 취소버튼(일반모드로 변경), [태그] 완료된 학습화분 이름, 커뮤니티 공유 버튼
//---------------
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "[${pot?.tag ?: "태그"}] ${pot?.name ?: "제목"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
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
                },
                actions = {
                    IconButton(onClick = {
                        if (uiState.isSelectionMode) {
                            if (uiState.selectedIds.isNotEmpty()) {
                                showDialog = true
                            } else {
                                Toast.makeText(context, "공유할 기록을 선택해 주세요.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            viewModel.toggleSelectionMode(true)
                        }
                    }) {

                        if (uiState.isSelectionMode) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "확인",
                                modifier = Modifier.size(24.dp),
                                tint = fontColorSub
                            )
                        } else {
                            Icon(
                                painterResource(id = R.drawable.ic_share),
                                contentDescription = "공유",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )

        }//---------------  topBar 끝

    ) { innerPadding ->
        // ---------- 완료 화분 정보 : 화분 이미지, 시작일, 종료일, 총 공부 시간
        if (pot != null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // ---------- 완료 화분 정보 : 화분 이미지, 시작일, 종료일, 총 공부 시간

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
                Spacer(modifier = Modifier.weight(1f))

                if (uiState.isSelectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            //전체 선택
                            Checkbox(
                                //
                                checked = uiState.selectedIds.size == uiState.logs.size && uiState.logs.isNotEmpty(),
                                onCheckedChange = {
                                    viewModel.clickAllCheckbox()
                                }
                            )
                            Text("전체 선택", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "${uiState.selectedIds.size}개 선택됨",
                            style = MaterialTheme.typography.labelMedium,
                            color = fontColorSub
                        )
                    }
                }
//                    ----------
                // ---------- 기록 리스트
                if (uiState.logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "학습 기록이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = fontColor
                        )
                    }
                } else {
                    //학습 기록 리스트
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.logs) { log ->
                            Log.d("plantLog", "${log.title}")
//
                            val dateTime = log.createAt.toDate().toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
//
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable(
                                        enabled = isSelectionMode,
                                        onClick = { viewModel.toggleSelection(log.id) }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (uiState.isSelectionMode) {
                                        Checkbox(
                                            checked = uiState.selectedIds.contains(log.id),
                                            onCheckedChange = { viewModel.toggleSelection(log.id) },
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(16.dp)) {
                                        // --- 1층 : 제목, 시간---
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (log.title.isNotEmpty()) log.title else TimeFormatter.formatToKoreanDate(
                                                    dateTime
                                                ),
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // 2. 공부시간 (우측 고정)
                                            // 1층의 오른쪽 끝: 공부 시간 (A02)
                                            Text(
                                                text = "[${TimeFormatter.formatToDigitalClock(log.studyingTime)}]",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // 학습 상세 내용 -> 2줄 제한
                                            val combinedContent = log.contents
                                                .take(2)
                                                .joinToString("\n") { "• $it" }
                                            if (combinedContent.isNotEmpty()) {
                                                Text(
                                                    text = combinedContent,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = fontColor,
                                                    modifier = Modifier.fillMaxWidth(),

                                                    // 표시 줄 수
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
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
                            tag = tag,
                            title = title,
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