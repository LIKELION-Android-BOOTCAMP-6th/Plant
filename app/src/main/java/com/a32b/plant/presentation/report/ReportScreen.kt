package com.a32b.plant.presentation.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.report.ui.ReportContent
import com.a32b.plant.presentation.report.viewmodel.ReportEvent
import com.a32b.plant.presentation.report.viewmodel.ReportViewModel
import kotlinx.coroutines.delay

@Composable
fun ReportScreen(navController: NavController) {
    val viewModel: ReportViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 보이는 동안 1분마다 날짜 변경 확인(앱 복귀 시 즉시, 화면을 켜 둔 채 자정이 지나도 반영)
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refreshIfDateChanged()
                delay(60_000L)
            }
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
        ReportContent(
            state = state,
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onDateSelected = viewModel::selectDate,
            onTabSelected = viewModel::selectTab,
            onRetry = viewModel::retry
        )
    }
}
