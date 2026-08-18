package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.presentation.attendance.ui.AttendanceCheckDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.getLogoImage
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel
import com.a32b.plant.presentation.theme.*
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

    val hasNoPot = displayPot.id.isEmpty() || displayPot == Pot.EMPTY

    var showAttendanceDialog by remember { mutableStateOf(false) }
    if (showAttendanceDialog) {
        AttendanceCheckDialog(onDismiss = { showAttendanceDialog = false })
    }

    LoadableScreen(viewModel) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    userName = userName,
                    onAttendanceClick = { showAttendanceDialog = true }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MainPlantCard(
                    displayPot = displayPot,
                    hasNoPot = hasNoPot,
                    onStartClick = {
                        if(!hasNoPot) {
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
                        if(hasNoPot){
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
                            )                        }
                    } // 상세 기록 이동
                )
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
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
                    painter = painterResource(id = R.drawable.ic_daily_default),
                    contentDescription = "출석체크",
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
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
            .fillMaxWidth(0.9f)
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = sub_green1
        ),
        border = BorderStroke(1.dp, primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = sub_green3,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Text(
                text = if(hasNoPot) "공부 화분이 없습니다" else displayPot.name.ifEmpty { "실험용" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = fontColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            if(hasNoPot){
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ){
                    getLogoImage()
                }
            } else {
                ProfileImage(level = displayPot.level.ifEmpty { "Lv.1" }, size = 150)

            }
            Spacer(modifier = Modifier.height(16.dp))

            // 학습 시간
            Text(
                text = "오늘 학습 시간",
                style = MaterialTheme.typography.bodySmall,
                color = fontColorSub
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "00:00",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = fontColor,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "누적 학습 시간  2시간 30분",
                style = MaterialTheme.typography.bodySmall,
                color = fontColorSub
            )

            Spacer(modifier = Modifier.height(20.dp))

            //공부 시작 버튼
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasNoPot
            ) {
                Text("공부 시작")
            }

            Spacer(modifier = Modifier.height(8.dp))

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