package com.a32b.plant.ui.feature.studying.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingViewModel

@Composable
fun StudyingScreen(navController: NavController) {
    //뷰모델 연결해주기
    val viewModel : StudyingViewModel = viewModel()

    //이전 스택에서 보낸 값을 args에 넣어서 뽑아낼 수 있음
    val args = navController.currentBackStackEntry?.toRoute<Routes.Studying>()

    Column {
        Text(text = "potId: ${args!!.potId}")
        Text(text = "potId: ${args!!.tag}")
        Text(text = "potId: ${args!!.title}")
    }


}