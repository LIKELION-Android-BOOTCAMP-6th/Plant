package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.util.FIRST_STREAK_CHUNK_DAYS
import com.a32b.plant.domain.util.NEXT_STREAK_CHUNK_DAYS
import com.a32b.plant.domain.util.isStudyDay
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
        if (!isStudyDay(totalMillis)) return Result.Success(0)

        var streakDays = 1
        var chunkEnd = date
        var chunkDays = FIRST_STREAK_CHUNK_DAYS
        while (true) {
            val chunkStart = chunkEnd.minusDays(chunkDays)
            val records = when (val result = getReportRecordsUseCase(uid, chunkStart, chunkEnd)) {
                is Result.Success -> result.data
                is Result.Failure -> return result
            }
            val totalsByDate = records.groupBy { it.startedAt.toLocalDate() }
                .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.studyingTime } }

            var day = chunkEnd.minusDays(1)
            while (!day.isBefore(chunkStart)) {
                if (!isStudyDay(totalsByDate[day] ?: 0L)) return Result.Success(streakDays)
                streakDays++
                day = day.minusDays(1)
            }
            chunkEnd = chunkStart
            chunkDays = NEXT_STREAK_CHUNK_DAYS
        }
    }
}
