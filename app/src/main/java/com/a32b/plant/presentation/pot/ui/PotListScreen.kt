package com.a32b.plant.presentation.pot.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.presentation.pot.viewmodel.PotListViewModel

@Composable
fun PotListScreen(
    navController: NavController,
    viewModel: PotListViewModel = hiltViewModel()
) {
    val pots by viewModel.pots.collectAsState()

    // 탭 상태 관리: 0 = 공부 중 (진행 중인 화분), 1 = 기른 화분 (완료된 화분)
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("공부 중", "기른 화분")

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            // 상단 타이틀 및 조건부 추가 버튼 영역
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Plant 나의 화분 이런식",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                // 공부 중 탭(selectedTab == 0)일 때만 플러스 버튼 노출
                if (selectedTab == 0) {
                    IconButton(
                        onClick = { navController.navigate(Routes.NewBornTree) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "화분 추가",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }
        },
        bottomBar = {
            // [중요] 홈 화면에서 바텀바가 정상 표출되는 컴포넌트(예: PlantBottomBar 등)로 교체해주세요.
        // PlantBottomBar(navController = navController)
    },
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (index < tabs.size - 1) {
                        Text(text = " | ", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 선택된 탭에 따라 필터링된 데이터
            val currentPots = pots.filter { if (selectedTab == 0) !it.isCompleted else it.isCompleted }

            // 3열 그리드 (LazyVerticalGrid) 적용
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(currentPots) { pot ->
                    PotGridItem(potName = pot.name)
                }
            }
        }
    }
}

@Composable
fun PotGridItem(potName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🌱", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = potName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "15:30:22",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}