package com.a32b.plant.ui.feature.studying.ui

import android.R.attr.textStyle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation.Companion.keyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.data.model.StudyingUser
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingEvent
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1
import com.a32b.plant.ui.theme.sub2
import com.a32b.plant.ui.theme.sub3
import java.time.LocalDateTime
import kotlin.math.log

@Composable
fun StudyingScreen(navController: NavController) {

    //이전 스택에서 보낸 값을 args에 넣어서 뽑아낼 수 있음
    val args = navController.currentBackStackEntry?.toRoute<Routes.Studying>()

    val tag = args!!.tagName
    val title = args.title
    val potId = args.potId
    val level = args.level

    val startTime = remember {
        val now = LocalDateTime.now()
        TimeFormatter.formatToTimeOnly(now) }
    val viewModel : StudyingViewModel = viewModel(factory = ViewModelFactory.studyingViewModelFactory(tag, potId, title, startTime, level))

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
                        level
                    )){
                        popUpTo(Routes.HomeMain) { inclusive = false }
                    }
                }
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
            StudyStatusBadge(tag, title)

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

            StudyingUserCard(studyingUsers, tag)
        }
    }
    if(uiState.isFinishDialogShown){
        viewModel.stopStopwatch()

        StudyFinishDialog(onDismiss = {viewModel.onDialogDismissClick()},onConfirm = { logs ->
            Log.d("입력값 확인", logs.toString())
            viewModel.onIsStudyFinishChange()
            viewModel.setStudyLog(logs)
            viewModel.onFinishStudyingClick()
        }, tag, title)
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
                                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
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