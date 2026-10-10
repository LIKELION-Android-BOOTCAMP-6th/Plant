package com.a32b.plant.domain.model

import java.time.LocalDate
import java.time.YearMonth

data class MonthlyReport(
    val month: YearMonth,
    val totalMillis: Long,
    val averageMillis: Long,
    val dailyTotals: List<ReportDailyTotal>,
    // 기록 있는 날이 없으면 둘 다 null. 하루뿐이면 둘 다 같은 날이다.
    val highestDay: ReportDailyTotal?,
    val lowestDay: ReportDailyTotal?,
    // 시작한 기록이 하나라도 있는 날짜(공부 시간이 0이어도 포함). 달력 점과 하루 평균의 일수에 쓴다.
    val recordDates: Set<LocalDate>
)

data class ReportDailyTotal(
    val date: LocalDate,
    val totalMillis: Long
)
