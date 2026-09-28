package com.a32b.plant.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyReport(
    val month: YearMonth,
    val totalMillis: Long,
    val averageMillis: Double,
    val averageDayCount: Int,
    val averageBasisDate: LocalDate,
    val dailyTotals: List<ReportDailyTotal>,
    val potComparison: ReportPotComparison,
    val calendarDates: Set<LocalDate>
)

data class ReportDailyTotal(
    val date: LocalDate,
    val totalMillis: Long
)
