package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.getLogoImage
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val displayPot by viewModel.displayPot.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val hasNoPot = displayPot.id.isEmpty() || displayPot == Pot.EMPTY

    LoadableScreen(viewModel) {
        Scaffold(
            topBar = { HomeTopBar(userName) }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MainPlantCard(
                    displayPot = displayPot,
                    hasNoPot = hasNoPot,
                    onStartClick = {
                        if (!hasNoPot) {
                            navController.navigate(Routes.Studying(
                                potId = displayPot.id,
                                tagId = displayPot.tagId,
                                tagName = displayPot.tagName,
                                title = displayPot.name,
                                level = displayPot.level
                            ))
                        }
                    },
                    onChangeOrMakeClick = {
                        if (hasNoPot) {
                            // 화분 없을 때 -> 화분 생성으로 이동
                            navController.navigate(Routes.NewBornTree)
                        } else {
                            // 화분 변경 다이얼로그
                            viewModel.setShowPotChangeDialog(true)
                        }
                    },
                    onRecordClick = {
                        if (!hasNoPot) {
                            navController.navigate(Routes.StudyPlanDetail(displayPot.id))
                        } // 상세 기록 이동
                    }
                )
            }
        }
    }
}

// 중복 코드 방지 헬퍼 함수
private fun displaypotIdOrDefault(id: String): String = id.ifEmpty { "default_pot" }

@Composable
fun HomeTopBar(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "${userName.ifEmpty{"사용자"}}의 Garden",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "공부가 자라나는 나만의 정원",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun MainPlantCard(
    displayPot: Pot,
    hasNoPot: Boolean,
    onStartClick: () -> Unit,
    onChangeOrMakeClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if(hasNoPot) "화분이 없습니다" else displayPot.name.ifEmpty { "실험용" },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if(hasNoPot){
                Box(
                    modifier = Modifier.size(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    getLogoImage()
                }
            } else {
                ProfileImage(level = displayPot.level.ifEmpty { "Lv.1" }, size = 150)
            }
            Spacer(modifier = Modifier.height(24.dp))

            //공부 시작 버튼
            Button(
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !hasNoPot
            ) {
                Text("공부 시작")
            }

            Spacer(modifier = Modifier.height(8.dp))

            //화분변경 / 기록 버튼 (가로 배치)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChangeOrMakeClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if(hasNoPot) "화분 생성" else "화분 변경")
                }
                OutlinedButton(
                    onClick = onRecordClick,
                    modifier = Modifier.weight(1f),
                    enabled = !hasNoPot
                ) {
                    Text("공부 기록")
                }
            }
        }
    }
}