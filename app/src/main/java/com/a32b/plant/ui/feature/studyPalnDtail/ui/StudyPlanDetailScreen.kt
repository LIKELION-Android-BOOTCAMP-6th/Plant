package com.a32b.plant.ui.feature.studyPalnDtail.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.ui.feature.studyPalnDtail.viewmodel.StudyPlanDetailViewModel
import com.a32b.plant.R
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.ui.theme.background
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import com.a32b.plant.ui.theme.title
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanDetailScreen(
    navController: NavController,
    viewModel: StudyPlanDetailViewModel = viewModel()
) {
    val potInfo by viewModel.potDetail.collectAsState()
    val logs by viewModel.studyLogs.collectAsState()

    // 제목 변경 다이얼로그 상태
    val isEditDialogShown by viewModel.isEditDialogShown.collectAsState()

    // 상세 기록 삭제 다이얼로그 상태
    val isDeleteDialogShown by viewModel.isDeleteDialogShown.collectAsState()

    //화분 전체 삭제 상태
    val isPotDeleteDialogShown by viewModel.isPotDeleteDialogShown.collectAsState()

    //임시 텍스트
    var editNameText by remember(isEditDialogShown) {
        mutableStateOf(potInfo?.name?: "")
    }

    // 선택한 로그 상태
    val selectedStudyLog by viewModel.selectedStudyLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    potInfo?.let {
                        Text("[${it.tag}] ${it.name}",
                            style = MaterialTheme.typography.titleMedium)
                    } ?: Text("로딩 중...")
                },
                navigationIcon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        //뒤로 가기
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_backbtn),
                                contentDescription = "뒤로가기",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        //삭제 버튼
                        TextButton(
                            onClick = {viewModel.setPotDeleteDialogShown(true)},
                            modifier = Modifier.height(20.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("삭제",
                                style = MaterialTheme.typography.labelSmall,
                                color = fontColorSub
                            )
                        }
                    }
                },
                actions = {
                    // 수정
                    IconButton(onClick = { viewModel.setEditDialogShown(true) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "수정하기",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* 공유 로직 */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "공유하기",
                            modifier = Modifier.size(24.dp)
                        )                    }
                }
            )
        },
        bottomBar = {
            // 학습 완료 버튼
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { /* 다이얼로그 노출 로직 */ }
            ) {
                Text("학습 완료하기")
            }
        }
    ) { innerPadding ->
        // 학습 기록 리스트 영역
        Box(modifier = Modifier.padding(innerPadding)) {
            //학습 기록 없을 시
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "아직 학습 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = fontColor
                    )
                }
            }
            //학습 기록 리스트
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(logs) { record ->
                    StudyRecordCard(
                        log = record,
                        onCardClick = { viewModel.onStudyLogClicked(record)},
                        onDeleteClick = {
                            viewModel.showDeleteDialog(record.id)
                        }
                    )
                }
            }
            // 선택 로그 존재 -> 다이얼로그 표출
            selectedStudyLog?.let { log ->
                StudyLogDetailDialog(
                    log = log,
                    ondismiss = {viewModel.onDismissLogDialog()}
                )
            }

            // 삭제 확인 다이얼로그
            if (isDeleteDialogShown){
                AlertDialog(
                    onDismissRequest = { viewModel.dismissDeleteDialog() },
                    title = { Text("기록 삭제") },
                    text = { Text("정말로 이 학습 기록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmDelete() }) {
                            Text("예", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                            Text("아니오", color = Color.Gray)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            // 제목 변경 다이얼로그
            if (isEditDialogShown) {
                AlertDialog(
                    onDismissRequest = { viewModel.setEditDialogShown(false) },
                    title = { Text("제목 변경", style = MaterialTheme.typography.titleMedium) },
                    text = {
                        OutlinedTextField(
                            value = editNameText,
                            onValueChange = { editNameText = it },
                            label = { Text("화분 이름") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.updatePotName(editNameText) },
                            enabled = editNameText.isNotBlank() // 빈 칸 저장 방지
                        ) {
                            Text("확인", fontWeight = FontWeight.Bold, color = Color(0xFFA5C16C))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setEditDialogShown(false) }) {
                            Text("취소", color = Color.Gray)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
            // 화분 전체 삭제 다이얼로그
            if(isPotDeleteDialogShown){
                AlertDialog(
                    onDismissRequest = {viewModel.setPotDeleteDialogShown(false)},
                    title = { Text("화분 삭제")},
                    text = {Text(" 이 화분과 화분의 \n \"모든 학습 기록\"이 영구 삭제됩니다. \n 정말 삭제하시겠습니까?")},
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.confirmDeleteEntirePot {
                                    navController.popBackStack()
                                }
                            }
                        ) {
                            Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setPotDeleteDialogShown(false) }) {
                            Text("취소", color = fontColor)
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun StudyRecordCard(
    log : StudyLog,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit){
    // Timestamp -> LocalDateTime 변환
    val dateTime = log.createAt.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable{ onCardClick()},
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                // 날짜
                Text(
                    text = if (log.title.isNotEmpty()) log.title else TimeFormatter.formatToKoreanDate(dateTime),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
                // 2. 공부시간 (우측 고정)
                Text(
                    text = "[${TimeFormatter.formatToDigitalClock(log.studyingTime)}]",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(5.dp))

//                 //삭제 버튼
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(painterResource(R.drawable.ic_trash),
                        contentDescription = "상세 공부 기록 삭제",
                        tint = Color.LightGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 학습 상세 내용 -> 2줄 제한
            val combinedContent = log.contents
                .take(2)
                .joinToString("\n") { "• $it" }
            if(combinedContent.isNotEmpty()){
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