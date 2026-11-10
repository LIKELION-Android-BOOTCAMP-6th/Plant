package com.a32b.plant.presentation.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.getLogoImage
import com.a32b.plant.presentation.core.extension.resId
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.home.viewmodel.HomeEvent
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel
import com.a32b.plant.presentation.theme.PlantTheme

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.presentation.core.component.ConfettiOverlay
import com.a32b.plant.presentation.core.component.festiveConfettiParties
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.theme.LocalIsDarkTheme
import com.a32b.plant.presentation.theme.primary
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    isSplashFinished: Boolean,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val displayPot by viewModel.displayPot.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val showPotChangeDialog by viewModel.showPotChangeDialog.collectAsState()
    val allPots by viewModel.allPots.collectAsState()
    val tempSelectedPot by viewModel.tempSelectedPot.collectAsState()
    val attendanceUiState by viewModel.attendanceUiState.collectAsState()
    val showAttendanceDialog by viewModel.showAttendanceDialog.collectAsState()
    val userItem by viewModel.userItem.collectAsState()
    val recoverySession by viewModel.recoverySession.collectAsState()
    val isRecovering by viewModel.isRecovering.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isBoxOpening by viewModel.isBoxOpening.collectAsState()
    var bonusBoxResult by remember { mutableStateOf<List<ItemType>?>(null) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showLevelUpConfetti by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val todayStudyTime by viewModel.todayStudyTime.collectAsState()
    val unseenStudyReward by viewModel.unseenStudyReward.collectAsState()

    val hasNoPot = displayPot.id.isEmpty() || displayPot == Pot.EMPTY

    // 홈이 보일 때마다 복구 세션 확인과 자동 출석 판단을 한 함수에서 순서대로 처리
    // LifecycleEventObserver는 등록 시점에 현재 상태까지 이벤트를 재생하므로 첫 진입에도 ON_RESUME이 발화됨
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onHomeResumed()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    recoverySession?.let { session ->
        RecoveryDialog(
            session = session,
            isLoading = isRecovering,
            onRecover = { viewModel.recoverSession() },
            onDiscard = { viewModel.discardSession() }
        )
    }

    // 복구 창이 있으면 출석 창을 숨긴다 (복구 처리 후 showAttendanceDialog가 true로 남아 있으면 다시 나타난다)
    // 다이얼로그는 별도 창이라 스플래시 위에 뜰 수 있어, 스플래시가 끝난 뒤에만 표시한다
    if (showAttendanceDialog && isSplashFinished && recoverySession == null) {
        AttendanceCheckDialog(
            uiState = attendanceUiState,
            onCheckClick = { viewModel.checkAttendance() },
            onDismiss = viewModel::closeAttendanceDialog
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

    if (uiState.showInsufficientItemsDialog) {
        ConfirmDialog(
            text = "아이템이 부족합니다",
            semiText = "아이템 상점에서 구매할 수 있어요.",
            onDismiss = { viewModel.onDismissInsufficientDialog() },
            onConfirm = { viewModel.onDismissInsufficientDialog() }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowToast -> context.showToast(event.message)
                is HomeEvent.BonusBoxResult -> bonusBoxResult = event.items
                is HomeEvent.LevelUpSuccess -> {
                    showLevelUpConfetti = true
                    context.showToast("레벨업 성공!")
                }
            }
        }
    }

    LoadableScreen(viewModel) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    userName = userName,
                    isAttendanceCompleted = attendanceUiState.isAttendanceCompleted,
                    // 중복 이동 방지용 안전장치: 맨 위가 이미 버그 제보 화면이면 새로 쌓지 않음
                    // (실기기 연타로는 두 번 쌓이는 현상이 재현되지 않았음)
                    onBugReportClick = {
                        navController.navigate(Routes.BugReport) { launchSingleTop = true }
                    },
                    onShopClick = viewModel::onShopClick,
                    onAttendanceClick = viewModel::openAttendanceDialog
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            // 홈 본문 배치 (#376) — 자세한 규칙은 HomeBodyLayout 참고
            // 화면 높이(maxHeight)를 본문 최소 높이로 넘겨, 남는 공간 계산의 기준으로 쓴다
            // 단, 최소 높이는 720dp까지만: 본문이 720dp 이하면 본문 전체를 기준으로 쓰고, 그보다 높으면(태블릿 등) 카드가 과하게 커지지 않게 720dp를 기준으로 계산한다
            // 본문은 위쪽 기준(TopCenter)으로 둬서 태블릿에서도 카드가 날짜 바로 아래에 오게 한다 (#566, 가운데 정렬이면 위쪽이 빔)
            // verticalScroll은 안전장치: 이미지를 최소로 줄여도 안 들어가는 극단적인 경우(큰 글꼴, 분할 화면 등 세로가 매우 짧을 때)에만 실제로 스크롤된다
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    // 상단(topBar) 여백만 적용: 하단바·내비바 여백은 MainActivity의 바깥 Scaffold가 처리한다 (#356)
                    .padding(top = paddingValues.calculateTopPadding())
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                // 간격 모드(compact)는 HomeBodyLayout이 "일반 모드로 화분이 다 들어가는지" 재 보고 정해서 넘겨준다 (#579)
                HomeBodyLayout(
                    modifier = Modifier
                        // 태블릿처럼 넓은 화면에서 카드·아이템 박스가 과하게 늘어나지 않게 최대 너비 제한
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight.coerceAtMost(720.dp))
                        // 좌우 여백 16dp: 앱바 타이틀·다른 탭 본문(내 화분, 마이페이지)과 왼쪽 끝을 맞춤
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { compact ->
                    MainPlantCard(
                        compact = compact,
                        displayPot = displayPot,
                        hasNoPot = hasNoPot,
                        todayStudyTime = todayStudyTime,
                        levelUpProgress = uiState.levelUpProgress,
                        levelUpProgressText = uiState.levelUpProgressText,
                        levelUpReady = uiState.levelUpReady,
                        isLoading = uiState.isLoading,
                        onStartClick = {
                            if (!hasNoPot) {
                                navController.navigate(
                                    Routes.Studying(
                                        potId = displayPot.id.ifEmpty { "default_pot" },
                                        tagId = displayPot.tagId,
                                        tagName = displayPot.tagName,
                                        title = displayPot.name.ifEmpty { "공부 목표" },
                                        level = "Lv.${displayPot.potLevel}"
                                    ))
                            }
                        },
                        onLevelUpClick = {
                            if (!hasNoPot && !uiState.isLoading) {
                                viewModel.onLevelUpClick(displayPot.id, displayPot.potLevel)
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
                    HomeItemBoxSection(
                        compact = compact,
                        item = userItem,
                        onItemClick = { clickedType ->
                            if (clickedType == ItemType.BOX) {
                                if (isBoxOpening) return@HomeItemBoxSection
                                if (userItem.box <= 0) {
                                    context.showToast("보유한 보너스 박스가 없습니다.")
                                } else {
                                    showOpenDialog = true // 오픈 수량 선택 다이얼로그 호출
                                }
                            }
                        }
                    )
                }
                //수량 선택 다이얼로그 (별도 창이라 본문 배치와 무관해 HomeBodyLayout 밖에 둔다)
                if (showOpenDialog) {
                    BonusBoxOpenDialog(
                        maxCount = userItem.box,
                        isOpening = isBoxOpening,
                        onDismiss = {if (!isBoxOpening) showOpenDialog = false },
                        onConfirm = { count ->
                            showOpenDialog = false
                            viewModel.openBoxes(count)
                        }
                    )
                }
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

        if (uiState.isLoading){
            LoadingBox()
        }
        bonusBoxResult?.let { items ->
            BonusBoxResultDialog(
                items = items,
                onDismiss = { bonusBoxResult = null }
            )
        }
        // 쌓인 학습 보상 (오프라인에서 저장한 세션, 결과 화면을 못 본 세션) — 서버가 지급한 뒤 합쳐서 한 번에 표시 (#453)
        // 복구·출석·박스 결과 창과 겹치지 않도록 다른 창이 없을 때만 띄운다
        val canShowStudyReward = isSplashFinished && recoverySession == null && !showAttendanceDialog &&
            attendanceUiState.reward == null && bonusBoxResult == null
        unseenStudyReward?.takeIf { canShowStudyReward }?.let { reward ->
            BonusBoxResultDialog(
                items = reward.toItemList(),
                title = "학습 보상이 도착했어요!",
                onDismiss = viewModel::confirmUnseenStudyRewards
            )
        }
        if (showLevelUpConfetti) {
            ConfettiOverlay(
                parties = remember { festiveConfettiParties() },
                onFinished = { showLevelUpConfetti = false}
            )
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
                style = MaterialTheme.typography.headlineMedium, //todo 지정된 스타일로 바꾸거나 스타일 새로 생성하기
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
                    color = MaterialTheme.colorScheme.onSecondary
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
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    // 목록이 비어 있을 때(예: 아직 목록을 한 번도 불러오지 못했을 때)는 안내 문구 아래에 바로 표시
                    NewPotSlot(onClick = onCreateNewClick)
                } else {
                    // weight(fill = false): 목록이 짧으면 내용 높이만큼만, 길면 남은 공간까지만 차지해서
                    // 본문 영역 최대 높이(heightIn(max = 300.dp))를 넘지 않고 그 안에서 스크롤되게 한다
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pots) { pot ->
                            val isSelected = pot.id == selectedPot?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPotSelected(pot) },
                                color = potSlotContainerColor(isSelected),
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
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "태그: ${pot.tagName.ifEmpty { "없음" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        text = "Lv.${pot.potLevel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if(isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                            }
                        }
                        // 목록 마지막 칸: 목록이 길면 끝까지 스크롤해야 보이는 새 화분 생성 칸
                        item {
                            NewPotSlot(onClick = onCreateNewClick)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedPot != null && selectedPot != Pot.EMPTY,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("선택 완료")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("취소")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// 화분 변경 다이얼로그의 칸 배경색 (화분 칸, 새 화분 만들기 칸 공용)
// 라이트는 surfaceVariant(sub2)·secondary(sub_green1)로 기존과 같고,
// 다크의 surfaceVariant는 밝은 색이라 onSurface 글자 대비가 약 2.3:1로 낮아 카드 색(primaryContainer)을 쓴다
@Composable
private fun potSlotContainerColor(isSelected: Boolean): Color = when {
    isSelected -> MaterialTheme.colorScheme.secondary
    LocalIsDarkTheme.current -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

// 화분 변경 다이얼로그의 '+ 새로운 화분 만들기' 칸 (화분 칸과 같은 모양)
@Composable
private fun NewPotSlot(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = potSlotContainerColor(isSelected = false),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+ 새로운 화분 만들기",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}


@Composable
fun HomeTopBar(
    userName: String,
    isAttendanceCompleted: Boolean,
    onBugReportClick: () -> Unit,
    onShopClick: () -> Unit,
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
            .padding(vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 왼쪽 16dp: 다른 탭 앱바(TabTopBar)와 같은 여백
                // 오른쪽 8dp: IconButton 안쪽 여백(48dp 중 그림 32dp → 8dp)과 합쳐
                // 아이콘 그림 끝 여백이 왼쪽과 같은 16dp가 되도록 함
                // (offset으로 밀면 레이아웃 자리는 그대로라 타이틀 쪽에 빈 공간이 생김)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 크기는 다른 탭 앱바(TabTopBar)와 같은 displayLarge 기본값(22sp)
            // 닉네임과 " 정원"을 나눠 닉네임만 말줄임 처리 → 좁은 기기·긴 닉네임에서도 " 정원"은 항상 보임
            Row(
                modifier = Modifier
                    .weight(1f)
                    // Text가 두 개라 TalkBack이 끊어 읽지 않도록 하나로 묶음
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    // fill = false: 짧은 닉네임일 때 " 정원"이 오른쪽 끝으로 밀려나지 않게 함
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " 정원",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }
            // 비공개 테스트 기간 버그 제보 바로가기 (#349) — 마이페이지 '버그 제보'와 같은 화면으로 이동
            // 상점 아이콘은 35dp 칸 중 실제 그림이 위아래 약 23dp라, 두 줄 글자 높이(약 22~23dp)를 그 그림에 맞춤
            // 터치 영역은 IconButton과 같은 48dp를 보장
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onBugReportClick)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "버그\n제보",
                    // 상점 아이콘 그림은 칸 중앙보다 약간 아래에 그려져 있어, 글자도 1dp 내려 위아래 끝을 맞춤
                    // (offset은 레이아웃 자리를 바꾸지 않아 터치 영역·옆 요소 배치에 영향 없음)
                    modifier = Modifier.offset(y = 1.dp),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
            IconButton(onClick = onShopClick) {
                // 투톤 색상은 drawable에 직접 지정 (출석체크 아이콘과 같은 방식)
                // 그림이 차지하는 비율이 출석체크보다 작아 35dp로 시각적 크기를 맞춤
                Image(
                    painter = painterResource(id = R.drawable.ic_shop),
                    contentDescription = "상점",
                    modifier = Modifier.size(35.dp)
                )
            }
            IconButton(onClick = onAttendanceClick) {
                Image(
                    painter = painterResource(
                        id = if (isAttendanceCompleted) {
                            R.drawable.plant_attendance_checked
                        } else {
                            R.drawable.plant_attendance_before
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
            // 날짜가 길어져도(글꼴 크게 설정 등) 화면 끝에 붙지 않도록 좌우 16dp (앱바 타이틀 여백과 동일)
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 홈 메인카드
 * 주의: HomeBodyLayout이 카드 최소 높이를 intrinsic(minIntrinsicHeight)으로 계산한다 (#376).
 * 카드 안에 LazyRow/LazyColumn, BoxWithConstraints 같은 SubcomposeLayout 기반 컴포넌트를 넣으면
 * 컴파일은 되지만 홈 화면을 열 때 런타임 크래시가 나므로 넣지 말 것.
 */
@Composable
fun MainPlantCard(
    displayPot: Pot,
    hasNoPot: Boolean,
    todayStudyTime: String,
    levelUpProgress: Float,
    levelUpProgressText: String,
    levelUpReady: Boolean,
    isLoading: Boolean,
    onStartClick: () -> Unit,
    onLevelUpClick: () -> Unit,
    onChangeOrMakeClick: () -> Unit,
    onRecordClick: () -> Unit,
    // 화면 높이가 모자랄 때 여백·간격을 줄이는 모드 (#376)
    compact: Boolean = false
) {
    // 카드 높이는 HomeBodyLayout이 정해 준다 (남는 세로 공간만큼 카드가 늘어남, #566)
    // 그래서 wrapContentHeight를 쓰지 않는다: 쓰면 받은 높이를 무시하고 내용 크기로 줄어든다
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        //border = BorderStroke(1.dp, primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 카드가 늘어난 만큼 채우고, 내용은 세로 가운데에 모은다 (#566)
                .fillMaxHeight()
                .padding(horizontal = 20.dp, vertical = 14.dp.compactIf(compact)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //상단 태그
            if(!hasNoPot && displayPot.tagName.isNotEmpty()){
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = displayPot.tagName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp.compactIf(compact)))
            }
            Text(
                text = if(hasNoPot) "공부 화분이 없습니다" else displayPot.name.ifEmpty { "실험용" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(8.dp.compactIf(compact)))

            // 이미지 칸은 간격 모드에서만 줄어든다 (최대 크기 → 최소 크기, #376)
            // 일반 모드는 최소 = 최대라 항상 최대 크기: HomeBodyLayout이 최대 크기로 다 들어갈 때만 일반 모드를 고르기 때문 (#579)
            // weight(fill = false): 카드 안 다른 요소를 다 놓고 남은 높이까지만 차지
            if(hasNoPot){
                Box(
                    // 로고는 원형 배경이 칸을 꽉 채워 100dp로도 화분 칸 170dp와 보이는 크기가 비슷하므로 100dp 유지 (#476)
                    // 원형 배경이라 높이에 맞춰 정사각형 유지
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .shrinkableImageHeight(min = if (compact) 40.dp else 100.dp, max = 100.dp)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true),
                    contentAlignment = Alignment.Center
                ){
                    getLogoImage()
                }
            } else {
                // 화분 이미지는 Lv6까지 같은 배율로 넣으려고 480x480 캔버스 가운데에 작게 그려져 있다 (화분 테두리 폭 196px = 캔버스의 약 41%, #474)
                // 그래서 칸을 170dp로 키워 화분 폭이 약 69dp로 보이게 한다 (#476, 420dpi 실기기 캡처에서 182px ÷ 2.625)
                // 간격 모드 최소 높이: 40dp면 화분 폭이 약 16dp밖에 안 돼 70dp(화분 폭 약 29dp)로 키운다 (#476)
                // → 예전 Lv0 이미지(화분 폭 = 이미지의 약 72%)를 40dp 칸에 그렸을 때와 같은 폭을 기준으로 삼음
                // → 대신 세로가 아주 짧은 화면에서는 그만큼(최대 30dp) 바깥 스크롤이 더 생긴다 (HomeBodyLayout 4번 참고)
                ProfileImage(
                    level = "Lv.${displayPot.potLevel}",
                    size = 170,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .shrinkableImageHeight(min = if (compact) 70.dp else 170.dp, max = 170.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp.compactIf(compact)))

            // 화분 하나의 오늘 총 학습 시간
            Text(
                text = "오늘 학습 시간",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp.compactIf(compact)))
            Text(
                text = if(hasNoPot) "00:00:00" else todayStudyTime,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = 26.sp
            )
            Spacer(modifier = Modifier.height(5.dp.compactIf(compact)))

            if (!hasNoPot) {
                Spacer(modifier = Modifier.height(8.dp.compactIf(compact)))
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
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                        Text(
                            text = levelUpProgressText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp.compactIf(compact)))
                    LinearProgressIndicator(
                        progress = { levelUpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {}
                    )
                }

                Spacer(modifier = Modifier.height(8.dp.compactIf(compact)))

                Button(
                    onClick = onLevelUpClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = levelUpReady && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("화분 레벨업", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(5.dp.compactIf(compact)))


            //공부 시작 버튼
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasNoPot
            ) {
                Text("공부 시작", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(5.dp.compactIf(compact)))

            //화분변경 / 기록 버튼 (가로 배치)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeOrMakeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if(hasNoPot) "화분 생성" else "화분 변경", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(
                    onClick = onRecordClick,
                    modifier = Modifier.weight(1f),
                    enabled = !hasNoPot
                ) {
                    Text("공부 기록", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HomeItemBoxSection(
    item: Item,
    onItemClick: (ItemType) -> Unit,
    // 화면 높이가 모자랄 때 여백·아이콘을 줄이는 모드 (#376)
    compact: Boolean = false
) {
    val displayItems = listOf(
        ItemType.HEART to item.heart,
        ItemType.SUN to item.sun,
        ItemType.WATER to item.water,
        ItemType.FERTILIZER to item.fertilizer,
        ItemType.NUTRIENT to item.nutrient,
        ItemType.BOX to item.box
    )

    val totalCount = displayItems.sumOf { it.second }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp.compactIf(compact))
    ) {
        Text(
            text = "아이템 : ${totalCount}개",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(start = 10.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            // 상단 MainPlantCard와 같은 배경색으로 통일
            color = MaterialTheme.colorScheme.secondary,
            tonalElevation = 2.dp
        ) {
            // 6개를 한 줄에 놓는다 (#387)
            // 칸 폭은 화면 폭을 6등분하되, ITEM_MIN_WIDTH보다 좁아지는 화면에서는 최소 폭을 지키고 가로 스크롤로 넘긴다
            // (BoxWithConstraints는 SubcomposeLayout이지만, HomeBodyLayout은 아이템박스를 intrinsic 없이 바로 measure하므로 괜찮다)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp.compactIf(compact))
            ) {
                val density = LocalDensity.current
                val gapPx = with(density) { ITEM_GAP.roundToPx() }
                // px 단위로 내림해서 6칸 + 간격 합이 폭을 넘지 않게 한다 (반올림 오차로 1px짜리 스크롤이 생기는 것 방지)
                val fitWidthPx = (constraints.maxWidth - gapPx * (displayItems.size - 1)) / displayItems.size
                val itemWidth = maxOf(with(density) { fitWidthPx.toDp() }, ITEM_MIN_WIDTH)
                val scrollState = rememberScrollState()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp.compactIf(compact))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(ITEM_GAP)
                    ) {
                        displayItems.forEach { (type, count) ->
                            HomeItemTile(
                                type = type,
                                count = count,
                                compact = compact,
                                // 박스만 클릭해서 개봉, 나머지 아이템은 클릭 동작 없음
                                onClick = if (type == ItemType.BOX) {
                                    { onItemClick(type) }
                                } else {
                                    null
                                },
                                modifier = Modifier.width(itemWidth)
                            )
                        }
                    }
                    // 좁은 화면에서 스크롤이 생길 때만, 옆으로 더 있다는 걸 알려주는 스크롤바를 보여준다
                    if (scrollState.maxValue > 0) {
                        HorizontalScrollIndicator(scrollState = scrollState)
                    }
                }
            }
        }
    }
}

/** 아이템 칸 사이 간격 (#387) */
private val ITEM_GAP = 4.dp

/**
 * 아이템 한 칸의 최소 폭 (#387)
 * 타일(최대 40dp) + 좌우 여유 1dp씩. 10sp 이름("영양제")도 이 폭 안에 들어간다.
 * (배지는 타일 안 오른쪽 아래에 두므로 이 여유를 쓰지 않는다)
 * 6칸이 이보다 좁아지는 화면(기기 폭 약 328dp 미만 = 6×42 + 간격 20 + 좌우 여백 56)에서는 가로 스크롤로 바뀐다.
 */
private val ITEM_MIN_WIDTH = 42.dp

/** 박스 타일 위 "열기" 문구가 들어갈 자리. 모든 칸에 똑같이 둬서 아이콘 줄을 맞춘다 (#387) */
private val BOX_CTA_SPACE = 8.dp

/**
 * 아이템 타일 한 칸: 정사각형 배경 + 아이콘 + 우측 하단 개수 배지, 아래에 이름 (#387)
 * 박스(클릭 가능)는 상단 "열기" 문구로 다른 아이템과 구분한다
 */
@Composable
private fun HomeItemTile(
    type: ItemType,
    count: Int,
    compact: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val tileMaxSize = if (compact) 36.dp else 40.dp
    val showOpenCta = onClick != null && count > 0
    // 화면 읽기용 설명: 칸 하나를 "정성 3개"처럼 한 번에 읽게 한다
    // 배지는 99개를 넘으면 "99+"로 보이지만, 읽을 때는 실제 개수를 읽어준다
    val accessibilityLabel = "${type.kor} ${count.coerceAtLeast(0)}개"

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            // 칸 안의 아이콘·배지·이름·"열기"를 하나로 묶고 위 설명만 읽게 한다 (안쪽 요소는 clearAndSetSemantics로 비움)
            // 박스 칸은 아래 clickable의 클릭 동작·onClickLabel이 같은 노드에 함께 남는다
            .semantics(mergeDescendants = true) { contentDescription = accessibilityLabel }
            .let { baseModifier ->
                if (onClick != null) {
                    baseModifier.clickable(
                        onClickLabel = "${type.kor} 열기",
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    baseModifier
                }
            }
            .padding(top = BOX_CTA_SPACE, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp.compactIf(compact))
    ) {
        Box(
            // widthIn을 fillMaxWidth보다 먼저 둬야 최대 크기가 적용된다
            // (반대 순서면 fillMaxWidth가 최소 폭을 칸 폭으로 고정해 widthIn(max)가 무시됨)
            modifier = Modifier
                .widthIn(max = tileMaxSize)
                .fillMaxWidth()
                .aspectRatio(1f)
                // 칸 전체 설명(accessibilityLabel)과 겹치지 않게 타일 안 글자·아이콘은 따로 읽지 않는다
                .clearAndSetSemantics { }
        ) {
            // 배경만 둥글게 자른다: 배지·"열기" 문구는 이 Box 밖(형제)에 두어 clip에 잘리지 않게 한다
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = type.resId),
                    // 칸 전체 설명(accessibilityLabel)에 이름이 들어가므로 아이콘은 따로 설명하지 않는다
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }
            if (showOpenCta) {
                BoxOpenCta(modifier = Modifier.align(Alignment.TopCenter))
            }
            // 배지는 타일 안 오른쪽 아래에 둔다
            // 바깥으로 빼면 칸이 최소 폭(42dp)일 때 칸의 clip(가운데 정렬이라 오른쪽 여유 1dp)에 잘린다 (#573 리뷰)
            ItemCountBadge(
                count = count,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        // 이름 자리는 모든 칸을 2줄 높이로 맞추고 글자를 세로 가운데에 둔다
        // → 1줄 이름(정성·햇빛…)이 "보너스" / "박스" 두 줄 사이 가운데 선에 맞춰 일자로 놓인다
        // 높이는 한 줄 높이를 dp로 바꾼 뒤 줄 수를 곱한다.
        // Android 14+는 글꼴을 키울 때 큰 sp일수록 덜 커져서(비선형), 28sp를 한 번에 바꾸면 실제 두 줄 높이보다 작아져
        // 글꼴 2배에서 1줄 이름이 약 5dp 위로 어긋났다 (#573 리뷰)
        Box(
            modifier = Modifier.heightIn(
                min = with(LocalDensity.current) { ITEM_LABEL_LINE_HEIGHT.toDp() * ITEM_LABEL_LINE_SLOTS }
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 줄마다 따로 그리고 각 줄은 1줄 + 말줄임으로 둔다
                // 한 Text에 maxLines = 2를 주면, 좁은 화면 + 큰 글꼴에서 "보너 / 스…"처럼 글자 중간에서 줄이 넘어가
                // "박스"가 사라졌다 (#573 리뷰)
                homeItemLabelLines(type).forEach { line ->
                    Text(
                        text = line,
                        // 칸 전체 설명(accessibilityLabel)과 겹치지 않게 이름은 따로 읽지 않는다
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = ITEM_LABEL_LINE_HEIGHT,
                            // 1줄짜리 Text는 기본으로 줄 높이의 위아래 여백을 잘라내서 "보너스" / "박스" 사이가 좁아진다.
                            // 잘라내지 않아 줄마다 ITEM_LABEL_LINE_HEIGHT 높이를 그대로 쓴다
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            )
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** 아이템 이름 한 줄 높이 (#387) */
private val ITEM_LABEL_LINE_HEIGHT = 14.sp

/** 아이템 이름 자리 높이에 쓰는 줄 수. "보너스" / "박스"가 2줄이라 모든 칸의 이름 자리를 2줄 높이로 맞춘다 (#387) */
private const val ITEM_LABEL_LINE_SLOTS = 2

/**
 * 홈 타일에 쓰는 이름을 줄 단위로 나눈 목록 (#387)
 * 보너스 박스 이름(ItemType.kor, "보너스 박스" 10sp 기준 약 52dp)은 한 칸(최소 42dp)에 한 줄로 들어가지 않아
 * 띄어쓰기 자리에서 나눠 2줄로 쓴다. 이름에 띄어쓰기가 없으면 1줄이 되고, 넘치면 말줄임된다.
 * 줄마다 1줄 + 말줄임으로 그리므로, 한 줄이 넘치면 그 줄만 "보…"처럼 줄어들고 다음 줄("박스")은 남는다.
 */
private fun homeItemLabelLines(type: ItemType): List<String> =
    if (type == ItemType.BOX) type.kor.split(" ") else listOf(type.kor)

/** "열기" 문구가 위아래로 움직이는 횟수 (#573 리뷰: 홈에 있는 내내 움직이지 않게 제한) */
private const val BOX_CTA_BOUNCE_COUNT = 3

/**
 * 보너스 박스 타일 위 "열기" 문구 (#387)
 * 처음 나타날 때 위로 2dp 올라갔다 내려오기를 BOX_CTA_BOUNCE_COUNT번 반복해 눌러서 열 수 있다는 걸 알려주고 멈춘다.
 * 홈에 다시 들어오거나 박스가 0개 → 1개 이상이 되어 다시 나타나면 처음부터 다시 움직인다.
 */
@Composable
private fun BoxOpenCta(modifier: Modifier = Modifier) {
    val bounce = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        repeat(BOX_CTA_BOUNCE_COUNT) {
            bounce.animateTo(-2f, animationSpec = tween(durationMillis = 600))
            bounce.animateTo(0f, animationSpec = tween(durationMillis = 600))
        }
    }
    Box(
        modifier = modifier
            // 타일 위쪽 가장자리에 걸치게 6dp 올린다
            // 위아래로 움직이는 2dp까지 더해도 BOX_CTA_SPACE(8dp) 안이라 칸의 clip에 잘리지 않는다
            .offset(y = -6.dp)
            // 위치는 그대로 두고 그리기만 움직여 레이아웃을 다시 계산하지 않게 한다
            .graphicsLayer { translationY = bounce.value.dp.toPx() }
            .clip(RoundedCornerShape(percent = 50))
            // 색은 배지와 같은 이유로 tertiary 바탕 + primaryContainer 글자 (ItemCountBadge 주석 참고)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 5.dp)
    ) {
        Text(
            text = "열기",
            style = fixedSizeTileTextStyle(),
            color = MaterialTheme.colorScheme.primaryContainer,
            maxLines = 1
        )
    }
}

/**
 * 가로 스크롤 위치를 보여주는 얇은 스크롤바 (#387)
 * 막대 길이 = 보이는 폭 / 전체 폭, 위치 = 현재 스크롤 비율. 스크롤 값은 그리기 단계에서만 읽어 스크롤 중 재구성을 피한다
 */
@Composable
private fun HorizontalScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    // primary는 다크 모드에서 아이템 박스 배경(secondary)과 대비가 약 1.1:1이라 막대가 거의 안 보여 tertiary 사용
    // (대비 계산값: 라이트 약 3.3:1, 다크 약 4.1:1)
    val trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
    val thumbColor = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .drawBehind {
                val total = scrollState.maxValue + scrollState.viewportSize
                if (total <= 0 || scrollState.maxValue <= 0) return@drawBehind
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(color = trackColor, cornerRadius = radius)
                val thumbWidth = size.width * scrollState.viewportSize / total
                val thumbX = (size.width - thumbWidth) * scrollState.value / scrollState.maxValue
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(thumbX, 0f),
                    size = Size(thumbWidth, size.height),
                    cornerRadius = radius
                )
            }
    )
}

/** 아이템 개수 배지: 0개도 표시하고, 99개를 넘으면 "99+"로 줄인다 (#387) */
@Composable
private fun ItemCountBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            // 한 자리일 때는 원, 두 자리 이상이면 가로로 늘어나는 알약 모양
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .clip(RoundedCornerShape(percent = 50))
            // primary + onPrimary는 글자 대비가 라이트 약 2.2:1, 다크 약 4.1:1로 10dp 글자에는 낮다 (#573 리뷰)
            // tertiary 바탕 + primaryContainer(타일 배경색) 글자로 바꿔 라이트 약 3.7:1, 다크 약 9.2:1 (대비 계산값)
            // primaryContainer는 원래 배경용 색이지만, 배지 글자가 타일 배경과 같은 색으로 뚫려 보이게 하려고 글자색으로 쓴다
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatItemBadgeCount(count),
            style = fixedSizeTileTextStyle(),
            color = MaterialTheme.colorScheme.primaryContainer,
            maxLines = 1
        )
    }
}

/**
 * 타일 위에 겹쳐 그리는 배지·"열기" 문구용 글자 스타일 (#387)
 * 크기를 dp로 고정해 시스템 글꼴 크기를 키워도 커지지 않게 한다.
 * 타일(최대 40dp)이 고정 크기라 글꼴 2배에서는 "99+"·"열기"가 칸 밖으로 넘쳐 잘렸기 때문 (아이템 이름은 글꼴 크기를 따른다)
 */
@Composable
// 이 파일은 java.time의 TextStyle도 import하고 있어 Compose TextStyle은 전체 경로로 쓴다
private fun fixedSizeTileTextStyle(): androidx.compose.ui.text.TextStyle = with(LocalDensity.current) {
    MaterialTheme.typography.titleSmall.copy(fontSize = 10.dp.toSp(), lineHeight = 12.dp.toSp())
}

/** 배지에 표시할 개수 문자열. 음수(비정상 데이터)는 0으로 표시 */
private fun formatItemBadgeCount(count: Int): String = when {
    count > 99 -> "99+"
    count < 0 -> "0"
    else -> count.toString()
}

@Preview(showBackground = true, name = "아이템 박스 - 일반 360dp", widthDp = 360)
@Preview(showBackground = true, name = "아이템 박스 - 작은 기기 320dp (가로 스크롤)", widthDp = 320)
@Preview(showBackground = true, name = "아이템 박스 - 태블릿 800dp", widthDp = 800)
@Composable
private fun HomeItemBoxSectionPreview() {
    PlantTheme {
        // 홈 본문과 같은 좌우 여백 16dp
        // 태블릿 미리보기는 홈 본문의 최대 너비(480dp) 제한 없이 그려, 타일 최대 크기 제한만 확인한다
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            HomeItemBoxSection(
                // 0개, 한 자리, 두 자리, 99, 100 이상(99+) 배지를 한 번에 확인
                item = Item(heart = 0, sun = 3, water = 12, fertilizer = 99, nutrient = 100, box = 1500),
                onItemClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "아이템 박스 - 간격 모드 360dp, 박스 0개", widthDp = 360)
@Composable
private fun HomeItemBoxSectionCompactPreview() {
    PlantTheme {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            HomeItemBoxSection(
                // 박스 0개: "열기" 문구가 없어야 한다
                item = Item(heart = 1, sun = 0, water = 5, fertilizer = 0, nutrient = 2, box = 0),
                onItemClick = {},
                compact = true
            )
        }
    }
}

@Composable
fun BonusBoxResultDialog(
    items: List<ItemType>,
    onDismiss: () -> Unit,
    title: String = "보너스 박스 개봉 결과!"
) {
    var showConfetti by remember { mutableStateOf(true) }

    val itemCounts = remember(items) {
        items.groupingBy { it }.eachCount().toSortedMap()
    }
    Box(modifier = Modifier.fillMaxSize()){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemCounts.forEach { (type, count) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = type.resId),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "${type.kor} x${count} 획득!",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("확인", style = MaterialTheme.typography.labelLarge)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
        if (showConfetti) {
            ConfettiOverlay(onFinished = { showConfetti = false })
        }
    }
}
@Composable
private fun RecoveryDialog(
    session: StudyingSession,
    isLoading: Boolean,
    onRecover: () -> Unit,
    onDiscard: () -> Unit
) {
    val studyTimeText = TimeFormatter.formatToDigitalClock(session.time)
    val dateText = TimeFormatter.formatTimeToDate(session.startedAt)

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "이전 학습 기록 발견",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (session.status) {
                        StudyingSessionStatus.STUDYING -> "앱이 비정상 종료되어 저장되지 않은 학습 기록이 있습니다."
                        StudyingSessionStatus.FINISHED -> "학습을 종료했지만 저장이 완료되지 않은 학습 기록이 있습니다."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "화분: ${session.title}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "날짜: $dateText",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "학습 시간: $studyTimeText",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onRecover,
                enabled = !isLoading
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                enabled = !isLoading
            ) {
                Text("폐기")
            }
        }
    )
}

@Composable
fun BonusBoxOpenDialog(
    maxCount: Int,
    isOpening: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var count by remember { mutableStateOf(1) }

    LaunchedEffect(maxCount) {
        if (count > maxCount) {
            count = maxCount.coerceAtLeast(1)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isOpening) onDismiss() },
        title = { Text("보너스 박스 개봉", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "보유한 박스: ${maxCount}개",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (count > 1) count-- },
                        enabled = count > 1 && !isOpening,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Text(
                        text = "${count}개",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(20.dp))

                    OutlinedButton(
                        onClick = { if (count < maxCount) count++ },
                        enabled = count < maxCount && !isOpening,
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }

                TextButton(
                    onClick = { count = maxCount },
                    enabled = count < maxCount
                ) {
                    Text("전체 선택 (MAX)", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(count) },
                enabled = !isOpening,
                colors = ButtonDefaults.buttonColors(containerColor = primary)
            ) {
                Text("${count}개 개봉하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isOpening) {
                Text("취소", color = MaterialTheme.colorScheme.onSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 홈 본문(메인카드 + 아이템박스) 배치 (#376)
 * content(compact)에는 [메인카드, 아이템박스] 순서로 정확히 두 개를 넣고, 받은 compact를 두 컴포넌트에 그대로 넘긴다.
 * 기준 높이 = 화면 높이(최대 720dp)에서 위아래 여백(8dp씩)을 뺀 값 (constraints.minHeight)
 *
 * 1. 간격 모드 판단 (#579): 먼저 일반 모드로 그려서 재 본다
 *    → 일반 모드 카드는 이미지 칸이 최대 크기로 고정이라(화분 170dp, 로고 100dp), 카드 최소 높이(minIntrinsicHeight)가
 *      "기준 높이 - 아이템박스 - 간격(18dp)" 이하면 이미지가 최대 크기로 다 들어가므로 일반 모드로 배치한다
 *    → 모자라면 간격 모드(여백·간격 절반, 아이템 타일 36dp)로 다시 그려서 배치한다
 *    → 화분 이름 길이·태그 유무·글꼴 크기로 내용이 길어지면 그만큼 더 높은 화면에서 간격 모드가 된다
 *    → 일반 모드는 이미지가 다 들어갈 때만 쓰므로 일반 모드에서는 바깥 스크롤이 생기지 않고,
 *      이미지 칸이 줄어드는 건 간격 모드뿐이라 본문이 낮아진다고 화분이 커지는 구간이 없다
 *    → SubcomposeLayout이라 간격 모드일 때 일반 모드 내용은 재기만 하고 배치하지 않는다 (화면·접근성 트리에 안 나옴)
 *      단 Compose 테스트 API(onAllNodes…)에는 배치되지 않은 내용도 잡히므로 홈 UI 테스트는 layoutInfo.isPlaced로 거를 것
 *    → 모드가 바뀌면 다른 slot이라 내용이 새로 만들어진다 ("열기" 문구 애니메이션·아이템 줄 가로 스크롤 위치가 처음부터 다시 시작)
 *      기준 높이·카드·아이템박스 높이가 바뀔 때만 생겨 허용한다 (#579)
 *      - 대표 화분이나 그 정보가 바뀌어 화분 유무·태그 유무·이름 줄 수가 달라질 때
 *      - 레벨업 등으로 진행률 문구(예: "40% (120분/300분)")의 줄 수가 달라질 때 (maxLines가 없어 좁은 화면·큰 글꼴에서 줄이 바뀔 수 있음)
 *      - 글꼴·화면 크기가 바뀔 때
 *      막으려고 두 상태를 slot 밖으로 올리면 간격 모드에서 두 slot이 같은 상태를 함께 쓰게 되므로 올리지 않는다
 *      (같은 Animatable을 두 LaunchedEffect가 animateTo하면 나중 호출이 앞의 애니메이션을 취소하고, ScrollState 하나가 두 horizontalScroll에 붙는다)
 * 2. 아이템박스는 원래 크기 그대로 둔다
 * 3. 카드는 "기준 높이 - 아이템박스 - 간격(18dp, 간격 모드 9dp)"만큼 높이를 받는다
 *    → 간격 모드에서 모자라면 카드 안 이미지가 줄어들어(shrinkableImageHeight) 한 화면에 다 담긴다
 * 4. 간격 모드에서 이미지를 최소로 줄여도 안 들어가면 카드 최소 높이(minIntrinsicHeight)를 그대로 주고,
 *    본문이 화면보다 길어져 바깥 verticalScroll로 내려 볼 수 있게 한다 (버튼이 잘리지 않게)
 *    → intrinsic 계산을 쓰므로 카드 안에 SubcomposeLayout 기반 컴포넌트(Lazy 목록, BoxWithConstraints 등)를
 *      넣으면 런타임 크래시가 난다 (MainPlantCard 주석 참고). 이 레이아웃 자체는 카드 바깥이라 괜찮다
 * 5. 카드는 맨 위(날짜 바로 아래)에 붙이고, 남는 세로 공간만큼 카드 자체를 늘린다 (#566)
 *    → 카드는 날짜 바로 아래, 아이템박스는 본문 아래쪽에 와서 두 박스가 본문을 고르게 채운다
 *    → 화분 유무로 카드 내용 높이가 달라도 빈 띠가 생기지 않고, 늘어난 공간은 카드 안 위아래 여백이 된다
 *    → 간격(18dp)은 그대로라 두 박스 사이가 벌어지지 않는다 (#376)
 *    → 단, 기준 높이는 최대 720dp라서 본문이 720dp보다 높으면(태블릿 등) 넘는 만큼은 아이템박스 아래 빈 공간으로 남는다
 */
@Composable
private fun HomeBodyLayout(
    modifier: Modifier = Modifier,
    content: @Composable (compact: Boolean) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        // 세로는 제한 없이 재고, 가로는 본문 너비에 맞춘다
        val childConstraints = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        // 스크롤 안이라 maxHeight는 무한대이고, 기준 높이는 minHeight로 받는다
        val targetHeight = constraints.minHeight

        // compact 값을 slot id로 써서 일반 모드·간격 모드 내용을 따로 만든다
        fun measureBody(compact: Boolean): HomeBodyMeasure {
            val measurables = subcompose(compact) { content(compact) }
            require(measurables.size == 2) { "HomeBodyLayout에는 메인카드와 아이템박스 두 개만 넣어야 합니다" }
            val (cardMeasurable, itemBoxMeasurable) = measurables
            val gap = 18.dp.compactIf(compact).roundToPx()
            val itemBox = itemBoxMeasurable.measure(childConstraints)
            return HomeBodyMeasure(
                cardMeasurable = cardMeasurable,
                itemBox = itemBox,
                gap = gap,
                cardAvailable = targetHeight - itemBox.height - gap,
                cardMinHeight = cardMeasurable.minIntrinsicHeight(constraints.maxWidth)
            )
        }

        // 1. 일반 모드로 이미지가 최대 크기로 다 들어가면 일반 모드, 아니면 간격 모드
        val normal = measureBody(compact = false)
        val body = if (normal.cardMinHeight <= normal.cardAvailable) normal else measureBody(compact = true)

        // 3·4. 카드 높이를 고정해서 준다: 공간이 남으면 카드가 늘어나고, 조금 모자라면(간격 모드) 그 높이 안에서 이미지가 줄어들고,
        // 최소 높이보다도 모자라면 최소 높이 그대로 둔다(바깥 스크롤)
        val cardHeight = maxOf(body.cardAvailable, body.cardMinHeight)
        val card = body.cardMeasurable.measure(
            childConstraints.copy(minHeight = cardHeight, maxHeight = cardHeight)
        )
        val itemBox = body.itemBox

        val contentHeight = card.height + body.gap + itemBox.height
        val layoutHeight = maxOf(targetHeight, contentHeight)

        layout(constraints.maxWidth, layoutHeight) {
            card.place((constraints.maxWidth - card.width) / 2, 0)
            itemBox.place((constraints.maxWidth - itemBox.width) / 2, card.height + body.gap)
        }
    }
}

/** HomeBodyLayout에서 한 모드로 잰 결과 (#579) */
private class HomeBodyMeasure(
    val cardMeasurable: Measurable,
    val itemBox: Placeable,
    val gap: Int,
    // 카드가 받을 수 있는 높이 = 기준 높이 - 아이템박스 - 간격
    val cardAvailable: Int,
    // 카드 최소 높이 (이미지 칸을 그 모드의 최소로 줄였을 때. 일반 모드는 최소 = 최대라 이미지 최대 크기 기준)
    val cardMinHeight: Int
)

/**
 * 받은 높이 안에서 [min]~[max] 사이로 높이를 정한다 (#376)
 * 공간이 넉넉하면 [max], 모자라면 받은 높이만큼 줄어들고, [min]보다 작아지지는 않는다.
 * "최소 몇 dp가 필요한지" 물어보면(minIntrinsicHeight) [min]으로 답해,
 * 바깥에서 카드의 최소 높이를 "이미지를 최소로 줄였을 때" 기준으로 계산할 수 있게 한다.
 */
private fun Modifier.shrinkableImageHeight(min: Dp, max: Dp): Modifier =
    this.then(ShrinkableHeightModifier(min, max))

// data class: min·max가 같으면 같은 modifier로 취급해, 다시 그릴 때마다 크기를 다시 재지 않게 한다
private data class ShrinkableHeightModifier(
    private val min: Dp,
    private val max: Dp
) : LayoutModifier {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val height = constraints.maxHeight
            .coerceAtMost(max.roundToPx())
            .coerceAtLeast(min.roundToPx())
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = min.roundToPx()

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = max.roundToPx()
}

/** 간격 모드일 때 여백·간격을 절반으로 줄인다 (#376) */
private fun Dp.compactIf(compact: Boolean): Dp = if (compact) this / 2 else this
