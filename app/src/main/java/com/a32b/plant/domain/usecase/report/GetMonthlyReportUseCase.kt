package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.model.ReportDailyTotal
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import java.time.YearMonth
import javax.inject.Inject

class GetMonthlyReportUseCase @Inject constructor(
    private val getReportRecordsUseCase: GetReportRecordsUseCase
) {
    suspend operator fun invoke(uid: String, month: YearMonth): Result<MonthlyReport> =
        getReportRecordsUseCase(uid, month.atDay(1), month.plusMonths(1).atDay(1)).map { records ->
            val totalsByDate = records.groupBy { it.startedAt.toLocalDate() }
                .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.studyingTime } }
            val dailyTotals = (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                ReportDailyTotal(date, totalsByDate[date] ?: 0L)
            }
            // 날짜 순 목록이라 같은 합계일 때 더 이른 날짜가 최다·최소 날로 뽑힌다.
            val recordDays = dailyTotals.filter { it.date in totalsByDate }
            val totalMillis = records.sumOf { it.studyingTime }

            MonthlyReport(
                month = month,
                totalMillis = totalMillis,
                averageMillis = if (recordDays.isEmpty()) 0L else totalMillis / recordDays.size,
                dailyTotals = dailyTotals,
                highestDay = recordDays.maxByOrNull { it.totalMillis },
                lowestDay = recordDays.minByOrNull { it.totalMillis },
                recordDates = totalsByDate.keys
            )
        }
}
