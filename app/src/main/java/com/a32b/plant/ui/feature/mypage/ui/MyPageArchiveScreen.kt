package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.ui.feature.home.ui.GridPlantItem
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveViewModel
import com.a32b.plant.ui.theme.background


@Composable
fun MyPageArchiveScreen(navController: NavController) {
    val viewModel: MyPageArchiveViewModel =
        viewModel(factory = ViewModelFactory.myPageArchiveViewModelFactory)
    val potList by viewModel.potList.collectAsState()

    Scaffold(
        topBar = {

        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 하단 그리드
            val chunkedPots = potList.chunked(3)
            items(chunkedPots) { rowPots ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPots.forEach { pot ->
                        GridPlantItem(
                            pot = pot,
                            modifier = Modifier.weight(1f),
                            // [콜백 1] 이미지 클릭 시 ->
                            onImageClick = {
                            },
                            // [콜백 2] 텍스트 클릭 시 ->
                            onTextClick = {

                            }
                        )
                    }
                    // 빈 칸 채우기 로직
                    val emptySlots = 3 - rowPots.size
                    repeat(emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Composable
    fun GridPlantItem(
        pot: PotInfo,
        modifier: Modifier = Modifier,
        onImageClick: () -> Unit, // 이미지 클릭 - 영역 클릭 시 로 변경하기
    ) {
        // 화분 ID가 비어있지 않을 때만 실제 내용을 표시
        if (!pot.id.isNullOrEmpty()) {
            Log.d("plantLog", "----- $pot")
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ProfileImage 내부에 Box를 감싸서 clickable을 적용합니다.
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp)) // 동그랗게 클릭 영역 제한
                        .clickable { onImageClick() } // [클릭 1] 이미지 클릭 시
                ) {
                    ProfileImage(
                        level = pot.level,
                        size = 60
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // 2. 화분 정보 (시간, 이름) - 학습 계획창 이동용
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        ,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 총 공부 시간
                    Text(
                        text = TimeFormatter.formatToDigitalClock(pot.potTotalStudyingTime ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                    // 화분 이름
                    Text(
                        text = pot.name ?: "이름 없음",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // 화분이 없는 빈 칸은 투명한 공간으로 둠 (그리드 정렬 유지용)
            Spacer(modifier = modifier.fillMaxWidth())
        }
    }
}