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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import java.time.ZoneId
import androidx.compose.ui.window.Dialog

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

    val isCompleteDialogShown by viewModel.isCompleteDialogShown.collectAsState()

    //임시 텍스트
    var editNameText by remember(isEditDialogShown) {
        mutableStateOf(potInfo?.name?: "")
    }

    // 선택한 로그 상태
    val selectedStudyLog by viewModel.selectedStudyLog.collectAsState()

    //전체 선택 상태
    val isAllSelected by remember(logs) {
        derivedStateOf {
            logs.isNotEmpty() && logs.all { it.isSelected }
        }
    }

    //공유 모든 상태
    val isShareMode by viewModel.isShareMode.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    titleContentColor = fontColor,
                    navigationIconContentColor = fontColor,
                    actionIconContentColor = fontColor
                ),
                title = {
                    potInfo?.let {
                        Text("[${it.tag}] ${it.name}",
                            style = MaterialTheme.typography.titleMedium)
                    } ?: Text("로딩 중...")
                },
                navigationIcon = {
                        //뒤로 가기
                        IconButton(onClick = {
                            if(isShareMode) viewModel.setShareMode(false)
                            else navController.popBackStack()
                        }) {
                            Icon(
                                painter = painterResource(
                                    id = if(isShareMode) R.drawable.ic_study_result_close
                                        else R.drawable.ic_backbtn
                                ),
                                contentDescription = if (isShareMode) "공유 취소" else "뒤로가기",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                },
                actions = {
                    // 수정
                    if(!isShareMode) {
                        IconButton(onClick = { viewModel.setEditDialogShown(true) }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                "수정하기",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                    IconButton(onClick = { if (!isShareMode) {
                        viewModel.setShareMode(true)
                    } else {
                        // 2. 이미 공유 모드라면 데이터 전송(화면 이동) 수행
                        viewModel.navigateToCommunityShare(navController)
                    } }) {
                        if (isShareMode) {
                            // 아이콘 벡터와 리소스 처리 분기
                            Icon(Icons.Default.Check, contentDescription = "확인", modifier = Modifier.size(19.dp), tint = fontColorSub)
                        } else {
                            Icon(painterResource(id = R.drawable.ic_share), contentDescription = "공유", modifier = Modifier.size(19.dp))
                        }
                    }
                }
            )
        },
        bottomBar = {
            // 학습 완료 버튼
            if(!isShareMode) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = potInfo?.isCompleted == false,
                    onClick = { viewModel.setCompleteDialogShown(true) }
                ) {
                    Text(if (potInfo?.isCompleted == true) "완료된 학습" else "학습 완료하기")
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
        ) {
            //전체 선택 체크 박스 + 삭제 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isShareMode) {
                    if (logs.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.toggleAllSelection(!isAllSelected) }
                        ) {
                            //전체 선택
                            Checkbox(
                                checked = isAllSelected,
                                onCheckedChange = { viewModel.toggleAllSelection(it) }
                            )
                            Text("전체 선택", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${logs.count { it.isSelected }}개 선택됨",
                            style = MaterialTheme.typography.labelMedium,
                            color = fontColorSub
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.setPotDeleteDialogShown(true) },
                        modifier = Modifier.height(20.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = "화분 전체 삭제",
                            modifier = Modifier.size(18.dp),
                            tint = fontColorSub
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            "화분 전체 삭제",
                            style = MaterialTheme.typography.labelSmall,
                            color = fontColorSub
                        )
                    }
                }
            }
            // 학습 기록 리스트 영역
            Box(modifier = Modifier.weight(1f)) {
                //학습 기록 없을 시
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 학습 기록이 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = fontColor
                        )
                    }
                } else {
                    //학습 기록 리스트
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs) { record ->
                            StudyRecordCard(
                                log = record,
                                isShareMode = isShareMode,
                                onSelectionChange = { isSelected ->
                                    viewModel.onLogSelectionChanged(record.id, isSelected)
                                },
                                onCardClick = {
                                    if (isShareMode) viewModel.onLogSelectionChanged(record.id, !record.isSelected)
                                    else viewModel.onStudyLogClicked(record)
                                },
                                onDeleteClick = {
                                    viewModel.showDeleteDialog(record.id)
                                }
                            )
                        }
                    }
                }
                // 선택 로그 존재 -> 다이얼로그 표출
                selectedStudyLog?.let { log ->
                    ConfirmDialog(
                        text = "상세 공부 기록",
                        semiText = log.contents.joinToString("\n") { "• $it" } +
                                "\n\n공부 시간: ${TimeFormatter.formatToDigitalClock(log.studyingTime)}",
                        onDismiss = { viewModel.onDismissLogDialog() },
                        onConfirm = { viewModel.onDismissLogDialog() }
                    )
                }
                // 삭제 확인 다이얼로그
                if (isDeleteDialogShown) {
                    ConfirmDialog(
                        text = "기록 삭제",
                        semiText = "정말로 이 학습 기록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
                        onDismiss = { viewModel.dismissDeleteDialog() },
                        onConfirm = { viewModel.confirmDelete() }
                    )
                }
                // 제목 변경 다이얼로그
                if (isEditDialogShown) {
                    Dialog(onDismissRequest = { viewModel.setEditDialogShown(false) }) {
                        // 공용 다이얼로그와 동일한 모양의 Card 생성
                        Card(
                            shape = RoundedCornerShape(30.dp),
                            colors = CardDefaults.cardColors(Color.White),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(22.dp)
                            ) {
                                Spacer(modifier = Modifier.height(10.dp))

                                // 제목
                                Text("제목 변경", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(16.dp))

                                // 입력창 (디자인에 어울리게 스타일링)
                                OutlinedTextField(
                                    value = editNameText,
                                    onValueChange = { editNameText = it },
                                    placeholder = { Text("화분 이름을 입력하세요", style = MaterialTheme.typography.bodyMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFFA5C16C), // 강조색 (primary)
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )

                                Spacer(modifier = Modifier.height(22.dp))

                                // 버튼 영역 (이미지 디자인 그대로 반영)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.setEditDialogShown(false) },
                                        modifier = Modifier.height(36.dp).weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F3EE)) // sub2 색상 느낌
                                    ) {
                                        Text("취소", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            if (editNameText.isNotBlank()) {
                                                viewModel.updatePotName(editNameText)
                                            }
                                        },
                                        modifier = Modifier.height(36.dp).weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C16C)) // primary 색상
                                    ) {
                                        Text("확인", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                // 화분 전체 삭제 다이얼로그
                if (isPotDeleteDialogShown) {
                    ConfirmDialog(
                        text = "화분 삭제",
                        semiText = "이 화분과 모든 학습 기록이 영구 삭제됩니다.\n정말 삭제하시겠습니까?",
                        onDismiss = { viewModel.setPotDeleteDialogShown(false) },
                        onConfirm = { viewModel.confirmDeleteEntirePot { navController.popBackStack() } }
                    )
                }

                // 학습 완료 확인 다이얼로그
                if (isCompleteDialogShown) {
                    ConfirmDialog(
                        text = "학습 완료",
                        semiText = "이 화분의 학습을 최종 완료 하시겠습니까?\n완료 후에는 '기른 나무'에서 확인 가능합니다.",
                        onDismiss = { viewModel.setCompleteDialogShown(false) },
                        onConfirm = {
                            viewModel.completeStudyPlan {
                                val potName = potInfo?.name ?: "화분"

                                android.widget.Toast.makeText(
                                    context,
                                    "\"$potName\"화분의 학습을 완료했어요!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            }
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun StudyRecordCard(
    log : StudyLog,
    isShareMode: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onCardClick: () -> Unit,
    onDeleteClick: () -> Unit){
    val dateTime = log.createAt.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable{ onCardClick()},
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(3.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(isShareMode) {
                Checkbox(
                    checked = log.isSelected,
                    onCheckedChange = onSelectionChange
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            //리스트 상세 다이얼로그
            Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                // 날짜
                Text(
                    text = if (log.title.isNotEmpty()) log.title else TimeFormatter.formatToKoreanDate(dateTime),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
                // 2. 공부시간 (우측 고정)
                Text(
                    text = "[${TimeFormatter.formatToDigitalClock(log.studyingTime)}]",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(5.dp))

                if(!isShareMode) {
                    //삭제 버튼
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_trash),
                            contentDescription = "상세 공부 기록 삭제",
                            tint = Color.LightGray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 학습 상세 내용 -> 2줄 제한
            val combinedContent = log.contents.take(2).joinToString("\n") { "• $it" }
                val finalContent = if (log.contents.size > 2) {
                    "$combinedContent\n..."
                } else {
                    combinedContent                }

                if(combinedContent.isNotEmpty()){
                    Text(
                        text = finalContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = fontColor,
                        modifier = Modifier.fillMaxWidth(),

                        // 표시 줄 수
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}