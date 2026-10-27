package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.util.MIN_STUDYING_TIME_MILLIS
import java.time.LocalDate
import javax.inject.Inject

class GetDailyReportUseCase @Inject constructor(
    private val getReportRecordsUseCase: GetReportRecordsUseCase
) {
    suspend operator fun invoke(uid: String, date: LocalDate): Result<DailyReport> {
        val recordsResult = getReportRecordsUseCase(uid, date, date.plusDays(1))
        // 시작 시각 순으로 정렬해 두면 같은 공부 시간일 때 먼저 시작한 기록이 최고·최저로 뽑힌다.
        val sortedRecords = when (recordsResult) {
            is Result.Success -> recordsResult.data.sortedBy { it.startedAt }
            is Result.Failure -> return recordsResult
        }
        val totalMillis = sortedRecords.sumOf { it.studyingTime }
        val streakDays = when (val result = getStreakDays(uid, date, totalMillis)) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }

        return Result.Success(
            DailyReport(
                date = date,
                totalMillis = totalMillis,
                highest = sortedRecords.maxByOrNull { it.studyingTime },
                lowest = if (sortedRecords.size < 2) null else sortedRecords.minByOrNull { it.studyingTime },
                records = sortedRecords,
                streakDays = streakDays
            )
        )
    }

    private suspend fun getStreakDays(uid: String, date: LocalDate, totalMillis: Long): Result<Int> {
        if (totalMillis < MIN_STUDYING_TIME_MILLIS) return Result.Success(0)

        var streakDays = 1
        var previousDate = date.minusDays(1)
        while (true) {
            val recordsResult = getReportRecordsUseCase(uid, previousDate, previousDate.plusDays(1))
            val previousRecords = when (recordsResult) {
                is Result.Success -> recordsResult.data
                is Result.Failure -> return recordsResult
            }
            val previousTotalMillis = previousRecords.sumOf { it.studyingTime }
            if (previousTotalMillis < MIN_STUDYING_TIME_MILLIS) return Result.Success(streakDays)

            streakDays++
            previousDate = previousDate.minusDays(1)
        }
    }
}
