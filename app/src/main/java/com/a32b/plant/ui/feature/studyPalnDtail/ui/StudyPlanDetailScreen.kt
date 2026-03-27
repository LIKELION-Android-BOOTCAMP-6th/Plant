package com.a32b.plant.ui.feature.studyPalnDtail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.ui.feature.studyPalnDtail.viewmodel.StudyPlanDetailViewModel
import com.a32b.plant.R
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.ui.theme.background
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.ui.theme.fontColor
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlanDetailScreen(
    navController: NavController,
    viewModel: StudyPlanDetailViewModel = viewModel()
) {
    val potInfo by viewModel.potDetail.collectAsState()
    val logs by viewModel.studyLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    potInfo?.let {
                        Text("[${it.tag}] ${it.name}",
                            style = MaterialTheme.typography.titleMedium)
                    } ?: Text("로딩 중...")
                },
                navigationIcon = {
                    //뒤로 가기
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    // 수정
                    IconButton(onClick = { /* 수정 로직 */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = "수정하기",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { /* 공유 로직 */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = "공유하기",
                            modifier = Modifier.size(24.dp)
                        )                    }
                }
            )
        },
        bottomBar = {
            // 학습 완료 버튼
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = { /* 다이얼로그 노출 로직 */ }
            ) {
                Text("학습 완료하기")
            }
        }
    ) { innerPadding ->
        // 학습 기록 리스트 영역
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(logs) { record ->
                StudyRecordCard(log = record)
            }
        }
    }
}
@Composable
fun StudyRecordCard(log : StudyLog){
    // Timestamp -> LocalDateTime 변환
    val dateTime = log.createAt.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    Card(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ){
                // 날짜
                Text(
                    text = if (log.title.isNotEmpty()) log.title else TimeFormatter.formatToKoreanDate(dateTime),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
                // 2. 공부시간 (우측 고정)
                Text(
                    text = "[${TimeFormatter.formatToDigitalClock(log.studyingTime)}]",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(5.dp))

//                 //삭제 버튼
//                IconButton(onClick = /* delete*/) {
//                    Icon(painterResource(R.drawable.ic_trash),
//                        contentDescription = "상세 공부 기록 삭제",
//                        tint = Color.LightGray
//                    )
//                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 학습 상세 내용 -> 2줄 제한
            val combinedContent = log.contents
                .take(2)
                .joinToString("\n") { "• $it" }
            if(combinedContent.isNotEmpty()){
                Text(
                    text = combinedContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = fontColor,
                    modifier = Modifier.fillMaxWidth(),

                    // 표시 줄 수
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}