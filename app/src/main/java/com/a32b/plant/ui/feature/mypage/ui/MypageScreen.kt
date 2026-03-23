package com.a32b.plant.ui.feature.mypage.ui

import android.R.attr.onClick
import android.R.attr.text
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.google.android.play.integrity.internal.n

@Composable
fun MypageScreen(navController: NavController) {
    val viewModel: MyPageViewModel = viewModel()
    val potId by viewModel.potId.collectAsState()
//    val nickname by viewModel.nickname.collectAsState()
    val nickname: String = "임시"
    var showDialog by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf("") }
    val context = LocalContext.current
//    LaunchedEffect(nickname) {
//        if (nickname.isNotBlank()) {
//            newNickname = nickname
//        }
//    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 프로필, 닉네임, 총 공부시간
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box( // 프로필이미지
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.LightGray, shape = CircleShape)
                    // 클릭 시 Dialog 띄우기
                    .clickable { showDialog = true }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1F)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                )
                {
                    Text(text = "$nickname 님")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "총 공부 시간")
                    Text(text = "20:43:30")
                }
            }

        }
//        -------
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 동작 확인 후 Dialog 에 넣기
            TextField(
                value = newNickname,
                onValueChange = { v -> newNickname = v },
                label = { Text("새 닉네임 입력") },
                placeholder = { Text("변경하실 닉네임을 입력해주세요") }
            )

            ButtonTemplate(text = "기른 나무 수-") { }
            ButtonTemplate(text = "커뮤니티 활동") { }
            ButtonTemplate(text = "앱 설정") { }
            ButtonTemplate(text = "공지사항") { }
            ButtonTemplate(text = "비밀번호 재설정") { }
            ButtonTemplate(text = "다크모드-") { }
        }
        Button(onClick = { navController.popBackStack() }) {
            Text("뒤로가기")
        }
        Text(text = "$potId 팟아이디")
    }
    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Text("Dialog 창 확인용")
        }
    }
}

@Composable
fun ButtonTemplate(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )


    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}