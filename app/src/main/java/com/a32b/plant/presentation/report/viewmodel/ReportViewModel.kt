package com.a32b.plant.presentation.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.model.PreparedReportMonth
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.report.CalculateDailyReportUseCase
import com.a32b.plant.domain.usecase.report.CalculateMonthlyReportUseCase
import com.a32b.plant.domain.usecase.report.PrepareReportMonthUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class ReportTab { DAILY, MONTHLY }

data class ReportUiState(
    val today: LocalDate = LocalDate.now(REPORT_ZONE),
    val month: YearMonth = YearMonth.from(today),
    val selectedDate: LocalDate = today,
    val tab: ReportTab = ReportTab.DAILY,
    val canGoNext: Boolean = false,
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val calculationError: Boolean = false,
    val calendarDates: Set<LocalDate> = emptySet(),
    val dailyReport: DailyReport? = null,
    val monthlyReport: MonthlyReport? = null
)

sealed class ReportEvent {
    data class ShowToast(val message: String) : ReportEvent()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val prepareReportMonthUseCase: PrepareReportMonthUseCase,
    private val calculateDailyReportUseCase: CalculateDailyReportUseCase,
    private val calculateMonthlyReportUseCase: CalculateMonthlyReportUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<ReportEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private var preparedMonth: PreparedReportMonth.Valid? = null
    private var requestJob: Job? = null
    private var requestId = 0

    init {
        loadMonth(_uiState.value.month, _uiState.value.selectedDate)
    }

    fun selectDate(date: LocalDate) {
        if (YearMonth.from(date) != _uiState.value.month) return
        _uiState.update { state ->
            state.copy(
                selectedDate = date,
                tab = ReportTab.DAILY,
                dailyReport = preparedMonth?.let { calculateDailyReportUseCase(it, date) }
            )
        }
    }

    fun selectTab(tab: ReportTab) {
        _uiState.update { it.copy(tab = tab) }
    }

    fun previousMonth() {
        val state = _uiState.value
        val previous = state.month.minusMonths(1)
        val today = LocalDate.now(REPORT_ZONE)
        loadMonth(previous, if (previous == YearMonth.from(today)) today else previous.atDay(1))
    }

    fun nextMonth() {
        val state = _uiState.value
        if (!state.canGoNext) return
        val next = state.month.plusMonths(1)
        val today = LocalDate.now(REPORT_ZONE)
        loadMonth(next, if (next == YearMonth.from(today)) today else next.atDay(1))
    }

    fun retry() {
        val state = _uiState.value
        if (state.loadError) loadMonth(state.month, state.selectedDate)
    }

    private fun loadMonth(month: YearMonth, selectedDate: LocalDate) {
        requestJob?.cancel()
        val currentRequest = ++requestId
        preparedMonth = null
        val today = LocalDate.now(REPORT_ZONE)
        _uiState.update {
            it.copy(
                today = today,
                month = month,
                selectedDate = selectedDate,
                // 달 이동 가능 여부는 여기서만 계산한다(화면은 이 값만 읽는다). 미래 달은 이동 불가.
                canGoNext = month < YearMonth.from(today),
                isLoading = true,
                loadError = false,
                calculationError = false,
                calendarDates = emptySet(),
                dailyReport = null,
                monthlyReport = null
            )
        }

        requestJob = viewModelScope.launch {
            ensureCurrentUserUseCase.invoke()
                .onSuccess { user -> fetchMonth(user.uid, month, currentRequest) }
                .onFailure {
                    if (currentRequest == requestId) finishWithLoadError()
                }
        }
    }

    private suspend fun fetchMonth(uid: String, month: YearMonth, currentRequest: Int) {
        reportRepository.getMonthData(uid, month)
            .onSuccess { monthData ->
                if (currentRequest != requestId) return@onSuccess
                when (val result = prepareReportMonthUseCase(month, monthData)) {
                    is PreparedReportMonth.Valid -> {
                        preparedMonth = result
                        val state = _uiState.value
                        _uiState.update {
                            it.copy(
                                calendarDates = result.calendarDates,
                                dailyReport = calculateDailyReportUseCase(result, state.selectedDate),
                                monthlyReport = calculateMonthlyReportUseCase(result, state.today),
                                isLoading = false
                            )
                        }
                        loaded()
                    }
                    PreparedReportMonth.CalculationError -> {
                        _uiState.update { it.copy(calculationError = true, isLoading = false) }
                        loaded()
                    }
                }
            }
            .onFailure { error ->
                if (currentRequest != requestId) return@onFailure
                // UnknownUser는 공용 세션 만료 처리에 맡기고, 그 외 조회 실패만 원인을 토스트로 안내한다.
                if (error is AppError.UnknownUser) ensureCurrentUserUseCase.invoke()
                else sendToast(error.message)
                finishWithLoadError()
            }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(ReportEvent.ShowToast(message)) }
    }

    private fun finishWithLoadError() {
        preparedMonth = null
        _uiState.update {
            it.copy(
                isLoading = false,
                loadError = true,
                calendarDates = emptySet(),
                dailyReport = null,
                monthlyReport = null
            )
        }
        loaded()
    }
}

private val REPORT_ZONE = ZoneId.of("Asia/Seoul")
