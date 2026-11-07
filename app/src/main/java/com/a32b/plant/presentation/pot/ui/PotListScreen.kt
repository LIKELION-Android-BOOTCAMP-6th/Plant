package com.a32b.plant.presentation.pot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.TabTopBar
import com.a32b.plant.presentation.pot.viewmodel.PotListViewModel

@Composable
fun PotListScreen(
    navController: NavController,
    viewModel: PotListViewModel = hiltViewModel()
) {
    val studyingPots by viewModel.studyingPots.collectAsState()
    val completedPots by viewModel.completedPots.collectAsState()

    // 탭 상태 관리: 0 = 공부 중 (진행 중인 화분), 1 = 기른 화분 (완료된 화분)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("공부 중", "기른 화분")

    val currentPots = if (selectedTab == 0) studyingPots else completedPots

    Scaffold(
        topBar = {
            TabTopBar(title = "내 화분") {
                // 공부 중 탭(selectedTab == 0)일 때만 플러스 버튼 노출
                if (selectedTab == 0) {
                    IconButton(
                        onClick = { navController.navigate(Routes.NewBornTree) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "화분 추가"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 하단바·내비바 여백은 MainActivity의 바깥 Scaffold가 이미 처리하므로 상단(topBar) 여백만 적용 (#355)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // 탭 레이아웃 ("공부 중 | 기른 화분")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, title ->
                    TextButton(onClick = { selectedTab = index }) {
                        Text(
                            text = title,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (index < tabs.size - 1) {
                        Text(text = " | ", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3열 그리드 (LazyVerticalGrid) 적용
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(currentPots) { pot ->
                    PotGridItem(
                        potName = pot.name,
                        potLevel = pot.potLevel,
                        totalStudyingTime = pot.potTotalStudyingTime,
                        isCompleted = pot.isCompleted,
                        onClick = {
                            navController.navigate(Routes.StudyPlanDetail(potId = pot.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PotGridItem(
    potName: String,
    potLevel: Int,
    totalStudyingTime: Long,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 카드 폭은 그리드 칸 폭에 맞추고, 높이는 이미지 + 레벨 글자만큼 늘어나게 둔다 (#477, #497)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            // 새 화분 이미지는 그림이 작게 들어 있어 칸 폭만큼 이미지 영역으로 사용
            // 큰 화면(폴드 펼침, 태블릿)에서 카드가 끝없이 커지지 않도록 최대 140dp로 제한
            val imageSize = minOf(maxWidth, 140.dp).value.toInt()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // Lv6은 그림이 이미지 위아래 끝까지 차 있어 카드 윗변·레벨 글자와 붙지 않도록 여백을 둔다
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            ) {
                ProfileImage(
                    level = "Lv.$potLevel",
                    size = imageSize
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Lv.$potLevel",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = potName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = TimeFormatter.formatToDigitalClock(totalStudyingTime),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
