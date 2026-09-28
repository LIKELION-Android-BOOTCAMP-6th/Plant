package com.a32b.plant.presentation.report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.report.ui.ReportContent
import com.a32b.plant.presentation.report.viewmodel.ReportEvent
import com.a32b.plant.presentation.report.viewmodel.ReportViewModel

@Composable
fun ReportScreen(navController: NavController) {
    val viewModel: ReportViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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
