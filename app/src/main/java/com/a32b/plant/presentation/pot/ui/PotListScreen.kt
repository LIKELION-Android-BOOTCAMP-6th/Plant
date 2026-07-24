package com.a32b.plant.presentation.pot.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.presentation.pot.viewmodel.PotListViewModel

@Composable
fun PotListScreen(
    viewModel: PotListViewModel = hiltViewModel(), navController: NavController
) {
    val pots by viewModel.pots.collectAsState()

    LazyColumn {
        item { Text("진행 중인 화분", style = MaterialTheme.typography.titleLarge) }
        items(pots.filter { !it.isCompleted }) { pot ->
            Text(text = pot.name) // 실제 아이템 컴포넌트로 교체
        }

        item { Text("완료된 화분", style = MaterialTheme.typography.titleLarge) }
        items(pots.filter { it.isCompleted }) { pot ->
            Text(text = pot.name) // 실제 아이템 컴포넌트로 교체
        }
    }
}