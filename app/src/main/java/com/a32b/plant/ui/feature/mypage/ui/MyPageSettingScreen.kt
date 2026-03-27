package com.a32b.plant.ui.feature.mypage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.a32b.plant.R

@Composable
fun MyPageSetting(navController: NavController) {

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
        ButtonTemplate(text = "탈퇴하기") { }
    }
}

