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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val isLoginError by viewModel.isLoginError.collectAsState()

    // 상태
    val displayPot by viewModel.displayPot.collectAsState()
    val showDialog by viewModel.showPotChangeDialog.collectAsState()
    val allPots by viewModel.allPots.collectAsState()
    if (isLoginError) {
        // 로그인 에러 시 보여줄 화면
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "정보를 가져오는데 실패했어요.\n앱 종료 후 다시 실행해주세요.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    } else {
        LoadableScreen(viewModel) {
        val displayPot by viewModel.displayPot.collectAsState()

        Scaffold(
            topBar = { HomeTopBar("사용자") }
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
                    onStartClick = {
                        navController.navigate(Routes.Studying(
                            potId = displayPot.id,
                            tagId = displayPot.tagId,
                            tagName = displayPot.tagName,
                            title = displayPot.name,
                            level = displayPot.level
                        ))
                    },
                    onChangeClick = { viewModel.setShowPotChangeDialog(true) }, // 팝업 오픈
                    onRecordClick = { navController.navigate(Routes.PotDetail(displayPot.id)) } // 상세 기록 이동
                )

                ItemStatusSection(displayPot = displayPot)
            }
            //팝업창
            if (showDialog) {
                PotChangeDialog(
                    pots = allPots,
                    tempSelectedPot = viewModel.tempSelectedPot.collectAsState().value,
                    onDismiss = { viewModel.setShowPotChangeDialog(false) },
                    onSelect = { viewModel.setTempSelectedPot(it) },
                    onConfirm = { viewModel.confirmPotChange() }
                )
            }
        }
    }
    }
}

@Composable
fun HomeTopBar(userName: String) {
    Text(
        text = "${userName}의 Garden",
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun MainPlantCard(
    displayPot: Pot,
    onStartClick: () -> Unit,
    onChangeClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f).padding(16.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = displayPot.name, style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(16.dp))
            ProfileImage(level = displayPot.level, size = 150)
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStartClick, modifier = Modifier.weight(2f)) {
                    Text("공부 시작")
                }
                OutlinedButton(onClick = onChangeClick, modifier = Modifier.weight(1f)) {
                    Text("변경")
                }
                OutlinedButton(onClick = onRecordClick, modifier = Modifier.weight(1f)) {
                    Text("기록")
                }
            }
        }
    }
}

@Composable
fun ItemStatusSection(displayPot: Pot) {
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("현재 화분: ${displayPot.name}의 보유 아이템 정보")
    }
}
@Composable
fun PotChangeDialog(
    pots: List<Pot>,
    tempSelectedPot: Pot?, // 현재 임시 선택된 화분
    onDismiss: () -> Unit,
    onSelect: (Pot) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("화분 변경") },
        text = {
            LazyColumn {
                items(pots) { pot ->
                    val isSelected = (pot.id == tempSelectedPot?.id)

                    // 테두리 표시를 위한 Surface 활용
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(pot) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        ListItem(
                            headlineContent = { Text(pot.name) },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = tempSelectedPot != null) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}