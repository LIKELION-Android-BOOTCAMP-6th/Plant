package com.a32b.plant.ui.feature.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(navController: NavController){
    //뷰모델 연결
    val viewModel: HomeViewModel = viewModel()

    val potId by viewModel.potId.collectAsState()
    val tag = viewModel.getTag()
    Column {
        Button(onClick = {
            navController.navigate(Routes.Studying(potId = potId , tag = tag, title = "정처기"))
        },
            enabled = potId!=null //potId 없으면 버튼 비활성화
        ) { Text("공부 시작") }
        Button(onClick = {navController.navigate(Routes.SignIn)}) { Text("로그인 화면으로 이동") }
        Text("user의 garden", style = MaterialTheme.typography.displayLarge)
    }


}