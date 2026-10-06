package com.a32b.plant.presentation.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.report.GetDailyReportUseCase
import com.a32b.plant.domain.usecase.report.GetMonthlyReportUseCase
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
    val dailyReport: DailyReport? = null,
    val monthlyReport: MonthlyReport? = null
)

sealed class ReportEvent {
    data class ShowToast(val message: String) : ReportEvent()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val getDailyReportUseCase: GetDailyReportUseCase,
    private val getMonthlyReportUseCase: GetMonthlyReportUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<ReportEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private var monthJob: Job? = null
    private var dailyJob: Job? = null

    init {
        loadMonth(_uiState.value.month, _uiState.value.selectedDate)
    }

    fun selectDate(date: LocalDate) {
        val state = _uiState.value
        if (YearMonth.from(date) != state.month) return
        // 실패 상태에서는 날짜만 바꾸고, 다시 시도 때 달·선택 날짜를 함께 조회한다.
        if (state.loadError) {
            _uiState.update { it.copy(selectedDate = date, tab = ReportTab.DAILY) }
            return
        }
        dailyJob?.cancel()
        _uiState.update { it.copy(selectedDate = date, tab = ReportTab.DAILY, isLoading = true, dailyReport = null) }
        ensureCurrentUserUseCase.invoke()
            .onSuccess { user -> dailyJob = viewModelScope.launch { fetchDailyReport(user.uid, date) } }
            .onFailure { finishWithLoadError() }
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

    // 한국 날짜가 바뀌었으면 보고 있던 달·선택 날짜 그대로 다시 조회한다(오늘·다음 달 버튼 갱신).
    fun refreshIfDateChanged() {
        val state = _uiState.value
        if (LocalDate.now(REPORT_ZONE) == state.today) return
        loadMonth(state.month, state.selectedDate)
    }

    fun retry() {
        val state = _uiState.value
        if (state.loadError) loadMonth(state.month, state.selectedDate)
    }

    private fun loadMonth(month: YearMonth, selectedDate: LocalDate) {
        monthJob?.cancel()
        dailyJob?.cancel()
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
                dailyReport = null,
                monthlyReport = null
            )
        }

        ensureCurrentUserUseCase.invoke()
            .onSuccess { user ->
                monthJob = viewModelScope.launch { fetchMonthlyReport(user.uid, month) }
                // 월간 조회가 곧바로 실패했으면 일간 조회를 새로 시작하지 않는다.
                if (_uiState.value.loadError) return@onSuccess
                dailyJob = viewModelScope.launch { fetchDailyReport(user.uid, selectedDate) }
            }
            .onFailure { finishWithLoadError() }
    }

    private suspend fun fetchMonthlyReport(uid: String, month: YearMonth) {
        getMonthlyReportUseCase(uid, month)
            .onSuccess { report ->
                // 다른 조회가 먼저 실패했으면 결과를 반영하지 않는다(실패 화면 유지).
                _uiState.update {
                    if (it.loadError) it
                    else it.copy(monthlyReport = report, isLoading = it.dailyReport == null)
                }
                loaded()
            }
            .onFailure { error -> failLoading(error) }
    }

    private suspend fun fetchDailyReport(uid: String, date: LocalDate) {
        getDailyReportUseCase(uid, date)
            .onSuccess { report ->
                // 다른 조회가 먼저 실패했으면 결과를 반영하지 않는다(실패 화면 유지).
                _uiState.update {
                    if (it.loadError) it
                    else it.copy(dailyReport = report, isLoading = it.monthlyReport == null)
                }
                loaded()
            }
            .onFailure { error -> failLoading(error) }
    }

    private fun failLoading(error: AppError) {
        // UnknownUser는 공용 세션 만료 처리에 맡기고, 그 외 조회 실패만 원인을 토스트로 안내한다.
        if (error is AppError.UnknownUser) ensureCurrentUserUseCase.invoke()
        else sendToast(error.message)
        finishWithLoadError()
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(ReportEvent.ShowToast(message)) }
    }

    // 달·날짜 중 하나만 실패해도 같은 실패 영역을 보여 주고, 남은 조회는 취소한다(다시 시도 때 둘 다 조회).
    private fun finishWithLoadError() {
        monthJob?.cancel()
        dailyJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                loadError = true,
                dailyReport = null,
                monthlyReport = null
            )
        }
        loaded()
    }
}

private val REPORT_ZONE = ZoneId.of("Asia/Seoul")
