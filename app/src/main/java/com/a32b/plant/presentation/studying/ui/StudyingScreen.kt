package com.a32b.plant.presentation.studying.ui

import android.graphics.drawable.PaintDrawable
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.studying.viewmodel.StudyingEvent
import com.a32b.plant.presentation.studying.viewmodel.StudyingViewModel
import com.a32b.plant.presentation.theme.Typography
import com.a32b.plant.presentation.theme.sub3
import com.a32b.plant.presentation.theme.title
import java.time.LocalDateTime

@Composable
fun StudyingScreen(navController: NavController, viewModel: StudyingViewModel = hiltViewModel()) {

    val context = LocalContext.current
    //이전 스택에서 보낸 값을 args에 넣어서 뽑아낼 수 있음
//    val args = navController.currentBackStackEntry?.toRoute<Routes.Studying>()
//
//    val tag = args!!.tagName
//    val title = args.title
//    val potId = args.potId
//    val level = args.level

    val startTime = remember {
        val now = LocalDateTime.now()
        TimeFormatter.formatToTimeOnly(now) }
    viewModel.onStartTimeChange(startTime)

    val uiState by viewModel.uiState.collectAsState()
    val timerButtonText = if (uiState.isStudying) "일시정지" else "학습하기"
    val timerButtonBack = if (uiState.isStudying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    BackHandler {
        viewModel.onFinishDialogShownChange()
    }

    LaunchedEffect(Unit) {
        viewModel.onStudyingUsersChange()
        viewModel.event.collect { event ->
            when(event) {
                is StudyingEvent.NavigateToStudyResult -> {
                    navController.navigate(Routes.StudyResult(
                        timestamp = event.timestamp,
                        tag = event.tag,
                        title = event.title,
                        log = event.log,
                        time = event.time,
                        potId = event.potId,
                        uiState.level
                    )){
                        popUpTo(Routes.HomeMain) { inclusive = false }
                    }
                }
                is StudyingEvent.NavigateToHome -> {
                    navController.navigate(Routes.HomeMain) {
                        popUpTo(0) { inclusive = true }
                    }
                }

                is StudyingEvent.ShowToast -> context.showToast(event.message)
            }
        }
    }



    val studyingUsers = uiState.studyingUsers
    Surface(modifier = Modifier.fillMaxSize(),
//        color = Color(0xFFF8F6F6)
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(40.dp))
            StudyStatusBadge(uiState.tag, uiState.title)

            Spacer(modifier = Modifier.height(70.dp))
            Text("$startTime ~", style = Typography.bodyMedium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetTimer(uiState.timer)

            Spacer(modifier = Modifier.height(30.dp))
            Row {
                //일시정지/학습시작 버튼
                StateChangeButton(timerButtonText, timerButtonBack){ viewModel.onStudyingStatusChange()}
                StateChangeButton("학습종료", MaterialTheme.colorScheme.secondary) {
                    viewModel.onFinishDialogShownChange()
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            StudyingUserCard(studyingUsers, uiState.tag)
        }
    }
    if (uiState.isGoalDialogShown){
        GoalInputDialog(
            tag = uiState.tag,
            title = uiState.title,
            studyLogs = uiState.studyLog,
            onDismiss = {viewModel.onGoalInputDialogChange(false)},
            onConfirm = {
                viewModel.setStudyLog(it)
                viewModel.onGoalInputDialogChange(false)
            }
        )
        /**
         onDismiss -> 스톱워치 시작, isStudying == true, 다이얼로그 닫기
         onConfirm -> 스톱워치 시작, isStudying == true, 다이얼로그 닫기, 스터디로그 비정상에 업데이트

         일단은
         0. 공부중 화면 진입
         1. 학습 목표 기입 다이얼로그 표출
         2. 닫으면 학습 기록 로컬 디비 업데이트 + 학습 시작
         3. 공부중 오늘 목표 칸을 클릭하면 -> 다이얼로그가 뜨면서 기입한 학습 목표가 뜸 + 없으면 "입력한 학습 목표가 없어요" 표출
         3-1. 수정 버튼도 존재하고, 수정 시 목표 기입 다이얼로그 표출
         4. 공부 종료 버튼 클릭 시 목표확인 다이얼로그 똑같이 뜨고 밑에 종료 버튼이 추가됨
         5. 공부 종료 시 해당 내용 그대로 학습 완료창으로 이동

         학습 목표 기입하는 다이얼로그는 그냥 여기서 만들면 됨
         학습 목표 확인하는 다이얼로그는 파일 새로 생성해서 만들고,
         비정상종료를 감지하는 파라미터 추가하기

         */

    }
    if(uiState.isFinishDialogShown){
        viewModel.onStudyingStatusChange(false)

        StudyFinishDialog(onDismiss = {viewModel.onDialogDismissClick()},onConfirm = { logs ->
            Log.d("입력값 확인", logs.toString())
            viewModel.onIsStudyFinishChange()
            viewModel.setStudyLog(logs)
            viewModel.onFinishStudyingClick()
        }, uiState.tag, uiState.title)
    }

    if (uiState.isLoading)
        LoadingBox()

    if (uiState.error != null)
        ConfirmDialog(
            text = "${uiState.error}",
            semiText = "메인 화면으로 이동합니다.",
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
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "[$tag] $title 공부중",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun SetTimer(time: Long){
    Box(modifier = Modifier.size(250.dp)
        .padding(16.dp),
        contentAlignment = Alignment.Center){
        Image(
            painter = painterResource(id = R.drawable.ic_studying_timebackground),
            contentDescription = "타이머",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )
        Text(text = "${TimeFormatter.formatToDigitalClock(time)}", style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface)
    }
}
@Composable
fun StateChangeButton(text: String, backColor: Color, function: () -> Unit){
    Card(
        modifier = Modifier.width(150.dp).height(70.dp).padding(10.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null){function()},
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = backColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center){
            Text("$text", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StudyingUserCard(users: List<StudyingUser>, tag: String){
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("$tag ${users.size}", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface)
            users.take(3).forEach { user ->
                StudyinUserItem(user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun StudyinUserItem(user: StudyingUser){
    Row(Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ProfileImage(user.profileImg, 30)
        Text(text = user.nickname, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp, start = 3.dp))
        Text(text = " ${TimeFormatter.formatToMinute(user.studyingTime)} 째 공부중!", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun GoalInputDialog(
    tag: String,
    title: String,
    studyLogs: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val maxLength = 100
    val maxLogSize = 10

    val localLogs = remember(studyLogs) {
        mutableStateListOf<String>().apply {
            if (studyLogs.isEmpty()) add("") else addAll(studyLogs)
        }
    }
    var index by remember { mutableIntStateOf(0) }
    val currentText = localLogs.getOrElse(index) { "" }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Column(
                modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text("[$tag] $title", style = Typography.titleSmall)

                Spacer(Modifier.height(7.dp))

                Text(
                    "${index + 1} / 10 ",
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 7.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center

                ) {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (index > 0) index--
                        },
                        enabled = index > 0
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_left),
                            contentDescription = "왼쪽",
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Box(modifier = Modifier
                        .weight(1f)
                        ) {
                        OutlinedTextField(
                            value = currentText,
                            shape = RoundedCornerShape(10.dp),
                            onValueChange = { newValue ->
                                if (newValue.length <= maxLength) {
                                    localLogs[index] = newValue
                                }
                            },
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                            placeholder = {
                                Text(
                                    text = "오늘의 학습 목표를 기록해보세요!",
                                    style = Typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            },
                            maxLines = 11,
                            textStyle = Typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            "${currentText.length}/$maxLength",
                            style = Typography.bodySmall,
                            color = if (currentText.length >= maxLength) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp)
                        )

                    }

                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            if (index < 10) index++
                        },
                        enabled = index < localLogs.size - 1
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_right),
                            contentDescription = "오른쪽",
                            modifier = Modifier.size(23.dp)
                        )
                    }

                }

                Row(
                    modifier = Modifier.padding(horizontal = 13.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (localLogs.size < maxLogSize) {
                                localLogs.add("")
                                index = localLogs.size - 1
                            }
                        },
                        enabled = localLogs.size < maxLogSize,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(33.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sub3, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("추가", style = Typography.bodySmall) }

                    Button(
                        onClick = {
                            if (localLogs.size > 1) {
                                localLogs.removeAt(index)
                                if (index >= localLogs.size) index = localLogs.size - 1
                            } else {
                                localLogs[0] = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(33.dp).padding(start = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(0.3f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("삭제", style = Typography.bodySmall) }


                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {onConfirm(localLogs.toList())},
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(33.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.7f), contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("완료", style = Typography.bodySmall)}

                }

                Row(modifier = Modifier.fillMaxWidth().padding(start = 7.dp),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            "다음에 입력하기",
                            style = Typography.bodySmall,
                            color = Color.Gray,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }


            }
        }
    }
}

@Composable
fun StudyFinishDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    tag: String,
    title: String
) {
    val inputs = remember { mutableStateListOf("") }  // 입력창 리스트
    val focus = remember { mutableStateListOf(FocusRequester()) } //포커스 조절하는 거

    val scrollState = rememberLazyListState()
    LaunchedEffect(inputs.size) {
        if (inputs.size > 1) {
            focus.last().requestFocus()
            scrollState.animateScrollToItem(inputs.size)
        }
    }
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)
                .consumeWindowInsets(WindowInsets.ime) //키보드 패딩
                .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("학습 종료", style = Typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface)

                        Text("[$tag] $title", style = Typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(21.dp))
                    }

                    // 입력창 리스트
                    itemsIndexed(inputs) { index, value ->
                        OutlinedTextField(
                            value = value,
                            shape = RoundedCornerShape(10.dp),
                            onValueChange = { inputs[index] = it },
                            modifier = Modifier.fillMaxWidth()
                                .focusRequester(focus[index]),
                            placeholder = {Text(
                                text = "오늘의 학습을 기록해보세요!",
                                style = Typography.bodyMedium,
//                                color = Color(0xFF858585))},
                                color = MaterialTheme.colorScheme.onSurfaceVariant)},
                            textStyle =Typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    inputs.add("")
                                    focus.add(FocusRequester())
                                }
                            )
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                    }

                    item {
                        // 플러스 버튼
                        IconButton(onClick = {
                            inputs.add("")
                            focus.add(FocusRequester())
                        }) {
                            Image(painter = painterResource(R.drawable.ic_studying_plus),
                                contentDescription = "추가 버튼",
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )

                        }

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
                // 취소 / 종료 버튼
                Row(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                    Button(onClick = onDismiss,
                        modifier = Modifier.height(45.dp).weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sub3,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Text("취소", style = Typography.bodyMedium) }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = { onConfirm(inputs.toList()) },
                        modifier = Modifier.height(45.dp).weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("종료", style = Typography.titleSmall) }
                }
            }

        }
    }
}
