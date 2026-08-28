package com.a32b.plant.presentation.studying.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.viewmodel.StudyingEvent
import com.a32b.plant.presentation.studying.viewmodel.StudyingViewModel
import com.a32b.plant.presentation.theme.Typography
import com.a32b.plant.presentation.theme.sub3
import java.time.LocalDateTime

@Composable
fun StudyingScreen(navController: NavController, viewModel: StudyingViewModel = hiltViewModel()) {

    val context = LocalContext.current

    val startTime = remember {
        val now = LocalDateTime.now()
        TimeFormatter.formatToTimeOnly(now) }
    viewModel.onStartTimeChange(startTime)

    val uiState by viewModel.uiState.collectAsState()
    val timerButtonText = if (uiState.isStudying) "일시정지" else "학습하기"
    val timerButtonBack = if (uiState.isStudying) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    BackHandler {
        viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.FINISH)
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
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(40.dp))
            StudyStatusBadge(uiState.tag, uiState.title)

            Spacer(modifier = Modifier.height(30.dp))
            Text("$startTime ~", style = Typography.bodyMedium, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            SetTimer(uiState.timer)

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.padding(10.dp)
                    .clickable{ viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.CHECK)},
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(vertical = 17.dp, horizontal = 13.dp).width(250.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text("오늘의 학습 목표", style = Typography.bodyMedium, textDecoration = TextDecoration.Underline )

                    Image(painter = painterResource(R.drawable.ic_right), contentDescription = "goalCheck")

                }

            }

            Spacer(modifier = Modifier.height(15.dp))

            Row {
                //일시정지/학습시작 버튼
                StateChangeButton(timerButtonText, timerButtonBack){ viewModel.onStudyingStatusChange()}
                StateChangeButton("학습종료", MaterialTheme.colorScheme.secondary) {
                    viewModel.onChangeGoalCheckMode(StudyingGoalCheckMode.FINISH)
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            StudyingUserCard(studyingUsers, uiState.tag)
        }
    }
    if (uiState.isGoalInputDialogShown){
        StudyingGoalInputDialog(
            isEditing = uiState.isEditing,
            initialIndex = uiState.logIndex ?: 0,
            tag = uiState.tag,
            title = uiState.title,
            studyLogs = uiState.studyLog.map { it.log },
            onDismiss = {
                viewModel.onGoalInputDialogChange(false)
                if (uiState.isEditing) viewModel.onIsEditingChanged(false, null)
            },
            onConfirm = {
                viewModel.setStudyLog(it)
                viewModel.onGoalInputDialogChange(false)
            },
            onConfirmEdit = {
                viewModel.onStudyLogChanged(index = uiState.logIndex ?: 0, log = it )
                viewModel.onGoalInputDialogChange(false)
                viewModel.onIsEditingChanged(false, index = null)
            }
        )
    }
    uiState.goalCheckMode?.let {
        StudyingGoalCheckDialog(
            mode = it,
            studyLog = uiState.studyLog,
            onEdit = {
                viewModel.onIsEditingChanged(index = it, value = true)
                viewModel.onGoalInputDialogChange(true)
             },
            onCompleted = {index, isCompleted ->
                viewModel.onStudyLogChanged(index, isCompleted)
            },
            onDismiss = {viewModel.onChangeGoalCheckMode(null)},
            onConfirm = {
                viewModel.onIsStudyFinishChange()
            }
        )
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
        Text(text = user.nickname, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp, start = 3.dp))
        Text(text = " ${TimeFormatter.formatToMinute(user.studyingTime)} 째 공부중!", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

