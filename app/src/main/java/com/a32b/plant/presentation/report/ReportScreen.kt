package com.a32b.plant.presentation.report

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.report.ui.ReportContent
import com.a32b.plant.presentation.report.viewmodel.ReportEvent
import com.a32b.plant.presentation.report.viewmodel.ReportViewModel

@Composable
fun ReportScreen(
    navController: NavController,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 STARTED로 돌아올 때 날짜 변경을 한 번 확인한다.
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshIfDateChanged()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is ReportEvent.ShowToast -> context.showToast(event.message)
            }
        }
    }

    LoadableScreen(viewModel = viewModel) {
        Scaffold(
            topBar = {
                Text(
                    "리포트",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    style = MaterialTheme.typography.displayLarge
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            ReportContent(
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
                state = state,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                onDateSelected = viewModel::selectDate,
                onTabSelected = viewModel::selectTab,
                onRetry = viewModel::retry
            )
        }
    }
}
