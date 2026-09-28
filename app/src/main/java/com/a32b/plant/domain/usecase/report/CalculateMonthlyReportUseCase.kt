package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.model.PreparedReportMonth
import com.a32b.plant.domain.model.ReportDailyTotal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class CalculateMonthlyReportUseCase @Inject constructor(
    private val calculatePotComparison: CalculatePotComparisonUseCase
) {
    operator fun invoke(prepared: PreparedReportMonth.Valid, today: LocalDate): MonthlyReport {
        val month = prepared.month
        val totalMillis = prepared.records.sumOf { it.studyingTime }
        val currentMonth = YearMonth.from(today) == month
        val averageDayCount = if (currentMonth) today.dayOfMonth else month.lengthOfMonth()
        val averageBasisDate = if (currentMonth) today else month.atEndOfMonth()
        val totalsByDate = prepared.records.groupBy { it.savedAt.toLocalDate() }
            .mapValues { (_, records) -> records.sumOf { it.studyingTime } }
        val dailyTotals = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            ReportDailyTotal(date, totalsByDate[date] ?: 0L)
        }

        return MonthlyReport(
            month = month,
            totalMillis = totalMillis,
            averageMillis = totalMillis.toDouble() / averageDayCount,
            averageDayCount = averageDayCount,
            averageBasisDate = averageBasisDate,
            dailyTotals = dailyTotals,
            potComparison = calculatePotComparison(prepared.records),
            calendarDates = prepared.calendarDates
        )
    }
}
