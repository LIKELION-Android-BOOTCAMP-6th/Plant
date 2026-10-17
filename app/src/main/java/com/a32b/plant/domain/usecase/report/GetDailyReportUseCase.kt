package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import java.time.LocalDate
import javax.inject.Inject

class GetDailyReportUseCase @Inject constructor(
    private val getReportRecordsUseCase: GetReportRecordsUseCase
) {
    suspend operator fun invoke(uid: String, date: LocalDate): Result<DailyReport> =
        getReportRecordsUseCase(uid, date, date.plusDays(1)).map { records ->
            // 시작 시각 순으로 정렬해 두면 같은 공부 시간일 때 먼저 시작한 기록이 최고·최저로 뽑힌다.
            val sortedRecords = records.sortedBy { it.startedAt }
            DailyReport(
                date = date,
                totalMillis = sortedRecords.sumOf { it.studyingTime },
                highest = sortedRecords.maxByOrNull { it.studyingTime },
                lowest = if (sortedRecords.size < 2) null else sortedRecords.minByOrNull { it.studyingTime },
                records = sortedRecords
            )
        }
}
