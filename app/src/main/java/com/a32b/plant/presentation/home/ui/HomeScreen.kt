package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.getLogoImage
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.home.viewmodel.HomeEvent
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel
import com.a32b.plant.presentation.theme.fontColor
import com.a32b.plant.presentation.theme.fontColorSub
import com.a32b.plant.presentation.theme.primary
import com.a32b.plant.presentation.theme.sub2
import com.a32b.plant.presentation.theme.sub_green1
import com.a32b.plant.presentation.theme.sub_green3
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val displayPot by viewModel.displayPot.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val showPotChangeDialog by viewModel.showPotChangeDialog.collectAsState()
    val allPots by viewModel.allPots.collectAsState()
    val tempSelectedPot by viewModel.tempSelectedPot.collectAsState()
    val attendanceUiState by viewModel.attendanceUiState.collectAsState()
    val context = LocalContext.current

    val hasNoPot = displayPot.id.isEmpty() || displayPot == Pot.EMPTY

    var showAttendanceDialog by remember { mutableStateOf(false) }
    if (showAttendanceDialog) {
        AttendanceCheckDialog(
            uiState = attendanceUiState,
            onCheckClick = { viewModel.checkAttendance() },
            onDismiss = { showAttendanceDialog = false }
        )
    }

    attendanceUiState.reward?.let { reward ->
        ConfirmDialog(
            text = reward.announceText(),
            isSingleBtn = true,
            onDismiss = viewModel::dismissAttendanceReward,
            onConfirm = viewModel::dismissAttendanceReward
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowToast -> context.showToast(event.message)
            }
        }
    }

    LoadableScreen(viewModel) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    userName = userName,
                    isAttendanceCompleted = attendanceUiState.isAttendanceCompleted,
                    onAttendanceClick = { showAttendanceDialog = true }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentAlignment = Alignment.Center
                ) {
                    MainPlantCard(
                        displayPot = displayPot,
                        hasNoPot = hasNoPot,
                        onStartClick = {
                            if (!hasNoPot) {
                                navController.navigate(
                                    Routes.Studying(
                                        potId = displayPot.id.ifEmpty { "default_pot" },
                                        tagId = displayPot.tagId,
                                        tagName = displayPot.tagName,
                                        title = displayPot.name.ifEmpty { "공부 목표" },
                                        level = displayPot.level.ifEmpty { "Lv.1" }
                                    ))
                            }
                        },
                        //화분 생성 이동 or 화분 변경 다이얼로그 오픈
                        onChangeOrMakeClick = {
                            if (hasNoPot) {
                                navController.navigate(Routes.NewBornTree)
                            } else {
                                viewModel.setShowPotChangeDialog(true)
                            }
                        },
                        onRecordClick = {
                            if (!hasNoPot) {
                                navController.navigate(
                                    Routes.StudyPlanDetail(
                                        potId = displayPot.id.ifEmpty { "default_pot" }
                                    )
                                )
                            }
                        } // 상세 기록 이동
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                HomeItemBoxSection()
            }

            // 화분 변경 다이얼로그
            if (showPotChangeDialog){
                PotChangeDialog(
                    pots = allPots,
                    selectedPot = tempSelectedPot,
                    onPotSelected = {
                        viewModel.setTempSelectedPot(it)
                    },
                    onDismiss = {
                        viewModel.setShowPotChangeDialog(false)
                    },
                    onConfirm ={
                        viewModel.confirmPotChange()
                    },
                    onCreateNewClick = {
                        viewModel.setShowPotChangeDialog(false)
                        navController.navigate(Routes.NewBornTree)
                    }
                )
            }
        }
    }
}

private fun AttendanceReward.announceText(): String = when (this) {
    is AttendanceReward.Coin -> "${amount}골드를 획득했습니다!"
    is AttendanceReward.ItemReward -> "${type.kor} ${amount}개를 획득했습니다!"
}

@Composable
fun PotChangeDialog(
    pots: List<Pot>,
    selectedPot: Pot?,
    onPotSelected: (Pot) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCreateNewClick: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = {
            Text(
                text = "화분 변경",
                style = MaterialTheme.typography.headlineMedium,
                color = fontColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Text(
                    text = "정원에 있는 다른 공부 화분으로\n변경할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fontColorSub
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (pots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "생성된 다른 화분이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = fontColorSub
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pots) { pot ->
                            val isSelected = pot.id == selectedPot?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPotSelected(pot) },
                                color = if (isSelected) sub_green1 else sub2,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = pot.name.ifEmpty { "이름 없는 화분" },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = fontColor
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "태그: ${pot.tagName.ifEmpty { "없음" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = fontColorSub
                                        )
                                    }
                                    Text(
                                        text = pot.level.ifEmpty { "Lv.0" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if(isSelected) primary else fontColorSub
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 새 화분 생성 버튼 (다이얼로그 내부 추가 옵션)
                TextButton(
                    onClick = onCreateNewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+ 새로운 화분 만들기",
                        color = primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedPot != null && selectedPot != Pot.EMPTY,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary
                )
            ) {
                Text("선택 완료")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = fontColorSub
                )
            ) {
                Text("취소")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}


@Composable
fun HomeTopBar(
    userName: String,
    isAttendanceCompleted: Boolean,
    onAttendanceClick: () -> Unit
) {
    val displayName = if (userName.isBlank()) "사용자" else userName

    //오늘 날짜 표출
    val currentDate = LocalDate.now()
    val year = currentDate.year
    val month = currentDate.monthValue
    val day = currentDate.dayOfMonth
    val dayOfWeek = currentDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
    val dateString = "${year}년 ${month}월 ${day}일 ${dayOfWeek}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${displayName}의 Garden",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            IconButton(
                onClick = onAttendanceClick,
                modifier = Modifier.offset(x = 6.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (isAttendanceCompleted) {
                            R.drawable.plant_attendance_checked
                        } else {
                            R.drawable.ic_daily_default
                        }
                    ),
                    contentDescription = "출석체크",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = dateString,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MainPlantCard(
    displayPot: Pot,
    hasNoPot: Boolean,
    onStartClick: () -> Unit,
    onChangeOrMakeClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = sub_green1
        ),
        //border = BorderStroke(1.dp, primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //상단 태그
            if(!hasNoPot && displayPot.tagName.isNotEmpty()){
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = sub_green1,
                    border = BorderStroke(1.dp, primary)
                ) {
                    Text(
                        text = displayPot.tagName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = sub_green3,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = if(hasNoPot) "공부 화분이 없습니다" else displayPot.name.ifEmpty { "실험용" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fontColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            if(hasNoPot){
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ){
                    getLogoImage()
                }
            } else {
                ProfileImage(level = displayPot.level.ifEmpty { "Lv.1" }, size = 100)

            }
            Spacer(modifier = Modifier.height(8.dp))

            // 화분 하나의 오늘 총 학습 시간
            Text(
                text = "오늘 학습 시간",
                style = MaterialTheme.typography.bodySmall,
                color = fontColorSub
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "00:00", // 나중에 DB에서 가져오기
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = fontColor,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(5.dp))

            if (!hasNoPot) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "업그레이드 진행도",
                            style = MaterialTheme.typography.bodySmall,
                            color = fontColorSub
                        )
                        Text(
                            text = "60% (30분/50분)", // 예시 텍스트 (추후 뷰모델 데이터와 연결)
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { 0.6f }, // 예시 진행률 (0.0f ~ 1.0f)
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = primary,
                        trackColor = sub2,
                        drawStopIndicator = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))


            //공부 시작 버튼
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasNoPot
            ) {
                Text("공부 시작")
            }

            Spacer(modifier = Modifier.height(5.dp))

            //화분변경 / 기록 버튼 (가로 배치)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeOrMakeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if(hasNoPot) "화분 생성" else "화분 변경")
                }
                OutlinedButton(
                    onClick = onRecordClick,
                    modifier = Modifier.weight(1f),
                    enabled = !hasNoPot
                ) {
                    Text("공부 기록")
                }
            }
        }
    }
}

@Composable
fun HomeItemBoxSection(){
    Column(
        modifier = Modifier.fillMaxWidth(),
           verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        //총 개수를 넣을지, 개수를 없앨지 고민 필요
        //추후 동적 연결 필요
        var itemNumber = 3

        Text(
            text = "아이템 : ${itemNumber}개",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = fontColor,
            modifier = Modifier.padding(start = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(5) { index ->
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(95.dp)
                            .clip(RoundedCornerShape(0.dp))
                            .background(sub_green1),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.bodySmall,
                            color = fontColor
                        )
                    }
                }
            }
        }
    }
}
