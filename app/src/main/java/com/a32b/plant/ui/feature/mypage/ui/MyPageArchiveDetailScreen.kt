package com.a32b.plant.ui.feature.mypage.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveDetailViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

//
// 학습 완료 화분 리스트 -> 화분 상세 창
// 나무 이미지
// 시작일 : 2026년 3월 00일
// 종료일 : 2026년 3월 00일
// 총 공부 시간 152: 25: 21
// 학습 logs


@Composable
fun MyPageArchiveDetailScreen(navController: NavController) {
    val args = navController.currentBackStackEntry?.toRoute<Routes.MyPageArchiveDetail>()!!
    val potId = args.potId

    val viewModel: MyPageArchiveDetailViewModel =
        viewModel(factory = ViewModelFactory.myPageArchiveDetailViewModelFactory(potId))
    val uiState by viewModel.uiState.collectAsState()
    val pot = uiState.pot


    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // 표준 상단바 높이
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 뒤로가기 버튼
                Icon(
                    painter = painterResource(id = R.drawable.ic_backbtn),
                    contentDescription = "뒤로가기",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )

                Text(
                    text = "[${pot?.tag ?: "태그"}] ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    ) { paddingValues ->
        if (pot != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProfileImage(
                        level = pot.level,
                        size = 100
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "시작일 : ${
                                pot.createdAt?.toDate()?.let {
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.KOREA
                                    ).format(it)
                                } ?: ""
                            }")
                        Text(
                            text = "종료일 : ${
                                pot.completedAt?.toDate()?.let {
                                    SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.KOREA
                                    ).format(it)
                                } ?: ""
                            }")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "총 공부 시간 : ${uiState.totalStudyTime}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}