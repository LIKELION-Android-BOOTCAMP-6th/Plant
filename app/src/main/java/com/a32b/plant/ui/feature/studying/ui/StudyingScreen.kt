package com.a32b.plant.ui.feature.studying.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyingUser
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1
import com.a32b.plant.ui.theme.sub2
import java.time.LocalDateTime

@Composable
fun StudyingScreen(navController: NavController) {
    //뷰모델 연결해주기
    val viewModel : StudyingViewModel = viewModel()
    val context = LocalContext.current

    //이전 스택에서 보낸 값을 args에 넣어서 뽑아낼 수 있음
    val args = navController.currentBackStackEntry?.toRoute<Routes.Studying>()

    val tag = args!!.tag
    val title = args!!.title
    val potId = args!!.potId

    val now = LocalDateTime.now()
    val startTime = TimeFormatter.formatToTimeOnly(now)

    var studyingUsers = mutableListOf<StudyingUser>()
    studyingUsers.add(StudyingUser("0", "닉네임", "lv.0", "자격증", 300000))
    studyingUsers.add(StudyingUser("0", "닉네임1", "lv.1", "자격증", 30000))
    studyingUsers.add(StudyingUser("0", "닉네임2", "lv.5", "자격증", 1000000))
    Surface(modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF8F6F6)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(40.dp))
            StudyStatusBadge(tag, title)

            Spacer(modifier = Modifier.height(70.dp))
            Text("$startTime ~")
            SetTimer(100)

            Spacer(modifier = Modifier.height(30.dp))
            Row {
                //일시정지/학습시작 버튼
                Button(viewModel.buttonText, viewModel.buttonBack){ viewModel.toggleStudyStatus()}
                Button("학습종료", sub1) { Toast.makeText(context, "학습 종료 버튼 ", Toast.LENGTH_SHORT).show() }
            }
            Spacer(modifier = Modifier.weight(1f))

            StudyingUserCard(studyingUsers)
        }
    }
}

@Composable
fun StudyStatusBadge(tag: String, title: String){
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.7.dp, color = primary),
        color = sub2
    ) {
        Text(
            text = "[$tag] $title 공부중",
            color = primary,
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
        Text(text = "$time")
    }
}
@Composable
fun Button(text: String, backColor: Color, function: () -> Unit){
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
            Text("$text")
        }
    }
}

@Composable
fun StudyingUserCard(users: List<StudyingUser>){
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomEnd = 0.dp),
        colors = CardDefaults.cardColors(containerColor = background)
        ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("자격증 5")
            users.forEach { user ->
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
        Text(text = user.nickname)
        Text(text = " ${TimeFormatter.formatToMinute(user.studyingTime)} 째 공부중!")
    }
}