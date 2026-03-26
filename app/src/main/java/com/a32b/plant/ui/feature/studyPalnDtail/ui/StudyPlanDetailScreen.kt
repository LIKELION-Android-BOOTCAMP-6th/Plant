package com.a32b.plant.ui.feature.studyPalnDtail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.feature.studyPalnDtail.viewmodel.StudyPlanDetailViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor


@Composable
fun StudyPlanDetailScreen(
    navController: NavController
) {
//    Scaffold(
//        topBar = {
//            // 1, 2, 3, 4, 5번이 포함된 상단 바
//            TopAppBar(
//                title = { Text(potInfo.tag) },
//                navigationIcon = {
//                    IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
//                },
//                actions = {
//                    IconButton(onClick = { /* 공유 기능 */ }) { Icon(Icons.Default.Share, contentDescription = null) }
//                }
//            )
//        },
//        bottomBar = {
//            // 8번 학습 완료하기 버튼
//            Button(
//                modifier = Modifier.fillMaxWidth().padding(16.dp),
//                onClick = { /* 다이얼로그 노출 로직 */ }
//            ) {
//                Text("학습 완료하기")
//            }
//        }
//    ) { innerPadding ->
//        // 6번 학습 기록 리스트 영역
//        LazyColumn(modifier = Modifier.padding(innerPadding)) {
//            items(records) { record ->
//                StudyRecordCard(record)
//            }
//        }
//    }
}