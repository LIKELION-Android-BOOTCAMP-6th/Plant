package com.a32b.plant.ui.feature.home.ui

import android.graphics.drawable.Icon
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButtonDefaults.elevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.component.LoadableScreen
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.feature.studying.ui.StudyingScreen
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub2


@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory.homeViewModelFactory)

    //화면전환 빈화면 -> 홈화면
    LoadableScreen(viewModel) {
        val userName by viewModel.userName.collectAsState()
        val currentDate by viewModel.currentDate.collectAsState() // 로컬 날짜 획득
        val displayPot by viewModel.displayPot.collectAsState()
        val potList by viewModel.potList.collectAsState()
        val interruptedUiState by viewModel.interruptedUiState.collectAsState() //공부중 비정상 종료 감지

        if (interruptedUiState.isInterrupted) {
            val tag = interruptedUiState.interruptedStudySession!!.tag
            val title = interruptedUiState.interruptedStudySession!!.title
            val time = interruptedUiState.interruptedStudySession!!.time
            InterruptedDialog(
                onDismiss = { viewModel.onInterruptedDialogDismiss() },
                onConfirm = { inputs ->
                    viewModel.setInterruptedStudyLog(inputs)
                    viewModel.saveStudyLog()
                    viewModel.onInterruptedDialogDismiss()
                },
                StudyingSession(CurrentUser.uid, tag = tag, title = title, time = time)
            )
        }
        Scaffold(
            topBar = { HomeTopBar(userName) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 50.dp)
            ) {
                // [홈 1 영역] 상단 메인 카드
                item {
                    HomeHeaderSection(currentDate, displayPot) {
                        if (!displayPot.id.isNullOrEmpty()) {
                            navController.navigate(
                                Routes.Studying(
                                    potId = displayPot.id!!,
                                    tagId = displayPot.tag_id ?: "",     // PotInfo의 tag_id
                                    tagName = displayPot.tag_name ?: "", // PotInfo의 tag_name
                                    title = displayPot.name ?: "",
                                    level = displayPot.level
                                )
                            )
                        }
                    }
                }

                // 하단 그리드
                items(potList.chunked(3)) { rowPots ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPots.forEach { pot ->
                            GridPlantItem(
                                pot = pot,
                                modifier = Modifier.weight(1f),
                                // 이미지 클릭 시 -> 메인 카드 교체
                                onImageClick = {
                                    viewModel.selectPot(pot)
                                },
                                // 텍스트 클릭 시 -> 학습 계획창으로 이동
                                onTextClick = {
                                    pot.id?.let { id ->
                                        navController.navigate(Routes.StudyPlanDetail(id))
                                    }

                                }
                            )
                        }

                        // 빈 칸 채우기 로직
                        val emptySlots = 3 - rowPots.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                // [추가 버튼 영역]
//             IconButton 대신 FloatingActionButton 사용
                item {
                    Spacer(modifier = Modifier.height(30.dp))

                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Routes.NewBornTree)
                        },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        containerColor = primary,
                        elevation = elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "화분 추가",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(userName: String) {
    Text(
        text = "${userName}의 Garden",
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun HomeHeaderSection(date: String, displayPot: PotInfo, onStartClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        Text(
            date,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(20.dp))
        MainPlantCard(displayPot = displayPot, onStartClick = onStartClick)
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            "아래로 내려서 화분 확인하기",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_down_two),
            contentDescription = "스크롤 안내",
            tint = Color.LightGray
        )

    }
}

@Composable
fun MainPlantCard(displayPot: PotInfo, onStartClick: () -> Unit) {
    val isPotEmpty = displayPot.id.isNullOrEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 화분이 있을 때만 태그 표시 (ID가 비어있지 않을 때)
            if (!isPotEmpty && !displayPot.tag_name.isNullOrEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displayPot.tag_name!!,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 화분이 없으면 안내 문구
            Text(
                text = if (isPotEmpty) "화분을 등록해보세요" else (displayPot.name ?: "이름 없음"),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 화분 이미지
            ProfileImage(
                level = displayPot.level,
                size = 150
            )

            Spacer(modifier = Modifier.height(40.dp))

            // [공부 시간] 화분이 없으면 00:00:00
            Text(
                text = TimeFormatter.formatToDigitalClock(displayPot.potTotalStudyingTime ?: 0L),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStartClick,
                // 화분이 있을 때 버튼을 활성화함
                enabled = !isPotEmpty,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
//                colors = ButtonDefaults.buttonColors(containerColor = primary,
//                    disabledContainerColor = Color.LightGray),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp,
                    disabledElevation = 0.dp
                )
            ) {
                Text(
                    text = if (isPotEmpty) "화분을 등록해주세요" else "공부 시작",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun GridPlantItem(
    pot: PotInfo,
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit,
    onTextClick: () -> Unit
) {
    // 화분 ID가 비어있지 않을 때만 실제 내용을 표시
    if (!pot.id.isNullOrEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 화분 그림 (ProfileImage) - 메인 카드 교체용
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick() }
            ) {
                ProfileImage(
                    level = pot.level,
                    size = 60
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 화분 정보 (시간, 이름) - 학습 계획창 이동용
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTextClick() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 총 공부 시간 (Medium 적용)
                Text(
                    text = TimeFormatter.formatToDigitalClock(pot.potTotalStudyingTime ?: 0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // 화분 이름 (Medium 적용)
                Text(
                    text = pot.name ?: "이름 없음",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // 화분이 없는 빈 칸은 투명한 공간으로 둠 (그리드 정렬 유지용)
        Spacer(modifier = modifier.fillMaxWidth())
    }
}

@Composable
fun InterruptedDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    studySession: StudyingSession
) {
    val inputs = remember { mutableStateListOf("") }
    val focus = remember { mutableStateListOf(FocusRequester()) }
    val scrollState = rememberLazyListState()

    LaunchedEffect(inputs.size) {
        if (inputs.size > 1) {
            focus.last().requestFocus()
            scrollState.animateScrollToItem(inputs.size)
        }
    }
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .consumeWindowInsets(WindowInsets.ime)
                    .imePadding()
            ) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            "이전 학습 기록이 저장되지 않았습니다!",
                            style = Typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "저장하시겠습니까?",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "[${studySession.tag}] ${studySession.title}",
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            TimeFormatter.formatToDigitalClock(studySession.time!!),
                            style = Typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    itemsIndexed(inputs) { index, value ->
                        OutlinedTextField(
                            value = value,
                            shape = RoundedCornerShape(10.dp),
                            onValueChange = { inputs[index] = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus[index]),
                            placeholder = {
                                Text(
                                    "학습을 기록해보세요!",
                                    style = Typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            textStyle = Typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    inputs.add("")
                                    focus.add(FocusRequester())
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        // 플러스 버튼
                        IconButton(onClick = {
                            inputs.add("")
                            focus.add(FocusRequester())
                        }, modifier = Modifier.size(30.dp)) {
                            Image(
                                painter = painterResource(R.drawable.ic_studying_plus),
                                contentDescription = "추가 버튼"
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("취소", style = Typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onConfirm(inputs.toList()) },
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("저장", style = Typography.bodyMedium)
                    }
                }

            }

        }
    }

}