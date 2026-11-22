package com.a32b.plant.presentation.studying.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.viewmodel.StudyingEvent
import com.a32b.plant.presentation.studying.viewmodel.StudyingViewModel
import com.a32b.plant.presentation.theme.LocalIsDarkTheme

@Composable
fun StudyingScreen(navController: NavController, viewModel: StudyingViewModel = hiltViewModel()) {

    val context = LocalContext.current

    val startTime = rememberSaveable { System.currentTimeMillis() }
    // ViewModel은 시작 시각이 비어 있을 때만 반영 — 백업에서 복원한 시작 시각은 덮어쓰지 않음
    viewModel.onStartedAtChange(startTime)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val timerButtonText = if (uiState.isStudying) "일시정지" else "학습하기"
    val timerButtonBack = if (uiState.isStudying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary

    // 종료 저장 중에도 비활성화하지 않음 — 비활성화하면 기본 뒤로 가기로 화면을 떠나 백업 삭제·결과 화면 이동이 취소됨
    // (서버 쓰기는 이미 반영됐는데 백업이 남아 홈 복구 창이 뜸. 다시 저장해도 같은 세션 ID라 중복 저장되지는 않음, #453)
    // 저장 중 호출은 ViewModel에서 무시
    // 홈이 보이면 타이머 서비스를 중지하므로(#514), 타이머를 유지한 채 홈으로 보내는 경로를 만들면 HomeViewModel.onHomeResumed를 함께 확인할 것
    BackHandler {
        viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.FINISH)
    }

    // 화면이 안 보이게 되면(홈 버튼·화면 끄기·최근 앱 목록에서 닫기) 일시정지 삭제 예약을 기다리지 않고 바로 지움 (#667)
    // 백그라운드에서는 몇 초 뒤 네트워크가 막혀 10초 뒤 삭제가 서버에 닿지 못함 (Android 16 에뮬레이터에서 확인)
    // 로컬 백업도 이때 바로 저장 — 백그라운드에서 시스템이 앱을 꺼도 주기 저장(30초)을 기다린 만큼 잃지 않게 (#668)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.onScreenStopped()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event) {
                is StudyingEvent.NavigateToStudyResult -> {
                    navController.navigate(Routes.StudyResult(
                        timestamp = event.timestamp,
                        tag = event.tag,
                        title = event.title,
                        log = event.log,
                        completed = event.completed,
                        time = event.time,
                        potId = event.potId,
                        level = uiState.level,
                        sessionId = event.sessionId,
                        isSavePending = event.isSavePending
                    )){
                        popUpTo(Routes.HomeMain) { inclusive = false }
                    }
                }
                is StudyingEvent.NavigateToHome -> {
                    navController.navigate(Routes.HomeMain) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is StudyingEvent.ShowToast -> context.showToast(event.message, event.duration)
            }
        }
    }



    val studyingUsers = uiState.studyingUsers
    // 목표 입력 다이얼로그도 로컬 복구 전에는 띄우지 않도록 함께 감쌈 (복구 시 다이얼로그가 깜빡이는 현상 방지)
    LoadableScreen(viewModel) {
        Surface(modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // 내용이 화면보다 길면 스크롤 (#673)
            // 최소 높이를 화면 높이로 잡아, 내용이 짧을 때는 weight 여백이 남은 공간을 채워 사용자 카드가 지금처럼 맨 아래에 붙음
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    Spacer(modifier = Modifier.height(40.dp))
                    StudyStatusBadge(uiState.tag, uiState.title)

                    if (!uiState.isLocalSaved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "자동 저장에 실패했습니다. 앱 종료 시 학습 기록이 유실될 수 있습니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                    // 저장되는 시작 시각(uiState.startedAt)을 표시 — 백업에서 복원한 세션·디버그 +1h도 기록과 같은 시각으로 보임
                    // 첫 화면에서는 아직 0일 수 있어(그대로 그리면 1970-01-01 0시, 한국 시간 "09:00"이 깜빡임) 그때만 화면을 연 시각으로 표시
                    Text("${TimeFormatter.formatToTimeOnly(uiState.startedAt.takeIf { it != 0L } ?: startTime)} ~", style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp,)
                    SetTimer(
                        time = uiState.timer,
                        color = if (uiState.goalCheckMode == StudyingGoalCheckMode.FINISH) MaterialTheme.colorScheme.error else Color.Unspecified
                    )

                    PausedLabel(
                        visible = !uiState.isStudying && uiState.goalCheckMode == null && !uiState.isGoalInputDialogShown
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier.padding(10.dp)
                            .clickable{ viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.CHECK)},
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        val goalIconSize = 23.dp
                        Box(modifier = Modifier.padding(vertical = 13.dp, horizontal = 13.dp).width(250.dp)) {
                            Text(
                                "오늘의 학습 목표",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                // 아이콘 너비 + 여백(8)을 양쪽에 똑같이 줘서 가운데 정렬 유지 + 큰 글꼴에서 아이콘과 겹침 방지
                                modifier = Modifier.align(Alignment.Center).padding(horizontal = goalIconSize + 8.dp)
                            )

                            Icon(
                                painter = painterResource(R.drawable.ic_right),
                                contentDescription = null,
                                modifier = Modifier.size(goalIconSize).align(Alignment.CenterEnd),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Row {
                        //일시정지/학습시작 버튼
                        StateChangeButton(timerButtonText, timerButtonBack){ viewModel.onStudyingStatusChange()}
                        StateChangeButton("학습종료", MaterialTheme.colorScheme.surface) {
                            viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.FINISH)
                        }
                    }

                    // [DEBUG] 보상 테스트용 — 릴리즈 전 제거
                    StateChangeButton("+1h [DEBUG]", MaterialTheme.colorScheme.errorContainer) {
                        viewModel.addDebugTime()
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    StudyingUserCard(studyingUsers, uiState.tag)
                }
            }
        }
        if (uiState.isGoalInputDialogShown){
            StudyingGoalInputDialog(
                isEditing = uiState.isEditing,
                initialIndex = uiState.logIndex ?: 0,
                tag = uiState.tag,
                title = uiState.title,
                logs = uiState.goalDraft,
                onLogsChange = viewModel::onGoalDraftChange,
                onDismiss = viewModel::onGoalInputDialogCloseRequest,
                onConfirm = {
                    viewModel.setStudyLog(it)
                    viewModel.onGoalInputDialogChange(false)
                },
                onConfirmEdit = confirmEdit@{ log ->
                    val index = uiState.logIndex ?: return@confirmEdit
                    viewModel.onStudyLogChanged(index = index, log = log)
                    viewModel.onGoalInputDialogChange(false)
                }
            )
        }
        // 입력 창 위에 띄워야 하므로 입력 창 다음에 둠 — 바깥 터치·뒤로 가기는 취소로 처리해 글 유지
        if (uiState.isGoalDiscardConfirmShown) {
            ConfirmDialog(
                text = "작성한 내용을 버릴까요?",
                confirmBtnText = "버리기",
                onDismiss = viewModel::onGoalDiscardCancel,
                onConfirm = viewModel::onGoalDiscardConfirm
            )
        }
        uiState.goalCheckMode?.let {
            StudyingGoalCheckDialog(
                mode = it,
                studyLog = uiState.studyLog,
                onEdit = { index -> viewModel.onStartEditing(index) },
                onCompleted = {index, isCompleted ->
                    viewModel.onStudyLogChanged(index, isCompleted)
                },
                onDismiss = {viewModel.onChangeGoalCheckMode(null)},
                onConfirm = {
                    viewModel.onIsStudyFinishChange()
                }
            )
        }
    }

    if (uiState.isLoading)
        LoadingBox()

    if (uiState.error != null)
        ConfirmDialog(
            text = "${uiState.error}",
            semiText = "메인 화면으로 이동합니다.",
            isSingleBtn = true,
            isCancelable = false,
            confirmBtnText = "확인",
            onDismiss = {},
            onConfirm = { viewModel.onErrorConfirmClicked() }
        )

    if (!uiState.isLocalSaved){
        //TODO 배너나 알림 창 같은 거 조그맣게 띄위기
    }
}

@Composable
fun StudyStatusBadge(tag: String, title: String){
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.7.dp, color = MaterialTheme.colorScheme.primary),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = "[$tag] $title 공부중",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

/**
 * 타이머 아래 "일시정지" 표시 (#287)
 *
 * 글자를 항상 그려 두고 숨길 때는 투명하게만 해서, 보이고 안 보일 때 높이가 같게 함 (아래 버튼이 움직이지 않게)
 * 높이는 최소 20dp — 글자가 더 높아지면 늘어남 (#673. 고정 20dp에서는 글꼴 1.8부터 글자 높이가 20dp를 넘고, 2.0에서 글자 아래가 잘리는 것을 확인)
 */
@Composable
internal fun PausedLabel(visible: Boolean) {
    Box(modifier = Modifier.heightIn(min = 20.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "일시정지",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            // 숨긴 글자는 TalkBack이 읽지 않게 semantics도 비움
            modifier = if (visible) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}
        )
    }
}

@Composable
fun SetTimer(time: Long, color: Color = Color.Unspecified){
    // 폭이 250dp보다 좁으면 폭에 맞춰 정사각형으로 줄어듦 (#673)
    Box(modifier = Modifier.widthIn(max = 250.dp)
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(16.dp),
        contentAlignment = Alignment.Center){
        Image(
            painter = painterResource(id = R.drawable.ic_studying_timebackground),
            contentDescription = "타이머",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )
        val timerStyle = MaterialTheme.typography.titleLarge
        // 큰 글꼴에서 원 밖으로 넘치지 않게 한 줄에 들어가는 크기까지만 줄임 (최대 titleLarge 크기)
        Text(
            text = TimeFormatter.formatToDigitalClock(time),
            style = timerStyle,
            color = color,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = timerStyle.fontSize)
        )
    }
}
@Composable
fun StateChangeButton(text: String, backColor: Color, function: () -> Unit){
    Card(
        modifier = Modifier.width(150.dp).padding(10.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null){function()},
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (LocalIsDarkTheme.current) 0.dp else 3.dp)
    ) {
        // 높이는 최소 50dp(바깥 여백 포함 70dp) — 글자가 줄바꿈되는 등 더 높아지면 늘어남 (#673)
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(8.dp),
            contentAlignment = Alignment.Center){
            Text("$text", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StudyingUserCard(users: List<StudyingUser>, tag: String){
    Card(
        // 최소 200dp — 내용이 더 길면 늘어남 (본문이 스크롤되므로 잘리지 않음, #673)
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("$tag ${users.size}", style = MaterialTheme.typography.titleSmall,)
            users.take(3).forEach { user ->
                StudyingUserItem(user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StudyingUserItem(user: StudyingUser){
    Row(Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ProfileImage(user.profileImg, 30)
        Text(text = user.nickname, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 3.dp, start = 3.dp))
        Text(text = " ${TimeFormatter.formatToMinute(user.studyingTime)} 째 공부중!", style = MaterialTheme.typography.bodyMedium,)
    }
}

