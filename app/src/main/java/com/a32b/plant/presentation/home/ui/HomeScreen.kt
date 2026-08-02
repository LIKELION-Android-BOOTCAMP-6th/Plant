package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val showPotChangeDialog by viewModel.showPotChangeDialog.collectAsState()
    val allPots by viewModel.allPots.collectAsState()
    val tempSelectedPot by viewModel.tempSelectedPot.collectAsState()

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
                            navController.navigate(Routes.NewBornTree)
                        } else {
                            viewModel.setShowPotChangeDialog(true)
                        }
                    },
                    onRecordClick = {
                        if (!hasNoPot) {
                            navController.navigate(Routes.StudyPlanDetail(displayPot.id))
                        }
                    }
                )
            }

            // 화분 변경 다이얼로그
            if (showPotChangeDialog) {
                PotChangeDialog(
                    pots = allPots,
                    selectedPot = tempSelectedPot,
                    onPotSelected = { viewModel.setTempSelectedPot(it) },
                    onDismiss = { viewModel.setShowPotChangeDialog(false) },
                    onConfirm = { viewModel.confirmPotChange() },
                    onCreateNewClick = {
                        viewModel.setShowPotChangeDialog(false)
                        navController.navigate(Routes.NewBornTree)
                    }
                )
            }
        }
    }
}

@Composable
fun PotChangeDialog(
    pots: List<Pot>,
    selectedPot: Pot?,
    onPotSelected: (Pot) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onCreateNewClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "화분 변경",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                Text(
                    text = "정원에 있는 다른 공부 화분으로\n변경할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (pots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("생성된 다른 화분이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pots) { pot ->
                            val isSelected = pot.id == selectedPot?.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPotSelected(pot) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = pot.name.ifEmpty { "이름 없는 화분" },
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "태그: ${pot.tagName.ifEmpty { "없음" }}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = pot.level.ifEmpty { "Lv.1" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 새 화분 생성 버튼 (다이얼로그 내부 추가 옵션)
                TextButton(
                    onClick = onCreateNewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ 새로운 화분 만들기")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedPot != null && selectedPot != Pot.EMPTY
            ) {
                Text("선택 완료")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
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
                text = if(hasNoPot) "공부 화분이 없습니다" else displayPot.name.ifEmpty { "실험용" },
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