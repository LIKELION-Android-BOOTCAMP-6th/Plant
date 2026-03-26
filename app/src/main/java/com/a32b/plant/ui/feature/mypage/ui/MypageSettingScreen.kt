package com.a32b.plant.ui.feature.mypage.ui

import android.R.attr.text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.theme.fontColor

@Composable
fun MypageSetting(navController: NavController) {

//    val viewModel: MyPageViewModel = viewModel(factory = ViewModelFactory.myPageViewModelFactory)
//    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backbtn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }
            Text(
                text = "앱 설정",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        ButtonTemplate(text = "이용약관") { }
        ButtonTemplate(text = "FAQ") { }
        ButtonTemplate(text = "사용설명서") { }
        ButtonTemplate(text = "앱 테마") { }
        // 🔥 여기 추가 (남은 공간 밀어내기)
        Spacer(modifier = Modifier.weight(1f))

        // 🔥 하단 + 우측 정렬
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = fontColor
                )
            ) { Text(text = "탈퇴하기") }
        }
    }
}

