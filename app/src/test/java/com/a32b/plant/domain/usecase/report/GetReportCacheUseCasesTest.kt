package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.ReportPeriodData
import com.a32b.plant.domain.model.ReportPotLogs
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class GetReportCacheUseCasesTest {
    @Test
    fun `records result carries server and cached source`() = runTest {
        val repository = FakeReportRepository(isFromCache = false)
        val useCase = GetReportRecordsUseCase(repository)

        val serverRecords = useCase("uid", date, date.plusDays(1)).getOrThrow()
        assertFalse(serverRecords.isFromCache)
        assertTrue(serverRecords.records.isEmpty())

        repository.isFromCache = true
        val cachedRecords = useCase("uid", date, date.plusDays(1)).getOrThrow()
        assertTrue(cachedRecords.isFromCache)
        assertTrue(cachedRecords.records.isEmpty())
    }

    @Test
    fun `monthly report carries source for server and cache results`() = runTest {
        val repository = FakeReportRepository(isFromCache = false)
        val useCase = GetMonthlyReportUseCase(GetReportRecordsUseCase(repository))

        assertFalse(useCase("uid", YearMonth.from(date)).getOrThrow().isFromCache)

        repository.isFromCache = true
        val cachedReport = useCase("uid", YearMonth.from(date)).getOrThrow()
        assertTrue(cachedReport.isFromCache)
        assertEquals(0, cachedReport.studyCount)
    }

    @Test
    fun `daily report is cache based when any streak chunk is cached`() = runTest {
        val repository = FakeReportRepository(periodData = { callIndex ->
            if (callIndex == 0) ReportPeriodData(listOf(currentDayLogs), isFromCache = false)
            else ReportPeriodData(emptyList(), isFromCache = true)
        })
        val useCase = GetDailyReportUseCase(GetReportRecordsUseCase(repository))

        val report = useCase("uid", date).getOrThrow()

        assertTrue(report.isFromCache)
        assertEquals(2, repository.calls)
    }

    @Test
    fun `daily report stays server based when all reads are server based`() = runTest {
        val repository = FakeReportRepository(periodData = { callIndex ->
            if (callIndex == 0) ReportPeriodData(listOf(currentDayLogs), isFromCache = false)
            else ReportPeriodData(emptyList(), isFromCache = false)
        })
        val useCase = GetDailyReportUseCase(GetReportRecordsUseCase(repository))

        assertFalse(useCase("uid", date).getOrThrow().isFromCache)
    }

    @Test
    fun `daily report with empty cached reads succeeds with no records`() = runTest {
        val repository = FakeReportRepository(isFromCache = true)
        val useCase = GetDailyReportUseCase(GetReportRecordsUseCase(repository))

        val report = useCase("uid", date).getOrThrow()

        assertTrue(report.isFromCache)
        assertTrue(report.records.isEmpty())
        assertEquals(0, report.streakDays)
    }

    private class FakeReportRepository(
        private val periodData: ((Int) -> ReportPeriodData)? = null,
        var isFromCache: Boolean = false
    ) : ReportRepository {
        var calls = 0

        override suspend fun getPeriodData(
            uid: String,
            startInclusive: Long,
            endExclusive: Long
        ): Result<ReportPeriodData> {
            val result = periodData?.invoke(calls) ?: ReportPeriodData(emptyList(), isFromCache)
            calls++
            return Result.Success(result)
        }
    }

    private fun <T> Result<T>.getOrThrow(): T = when (this) {
        is Result.Success -> data
        is Result.Failure -> throw AssertionError("Expected success, got $error")
    }

    private companion object {
        val date = LocalDate.of(2026, 11, 9)
        val currentDayLogs = ReportPotLogs(
            pot = Pot(
                id = "pot-1",
                tagId = "tag-1",
                tagName = "study",
                name = "Plant",
                potTotalStudyingTime = 60_000L,
                createdAt = null,
                completedAt = null,
                isCompleted = false,
                lastStudiedAt = null,
                potLevel = 0
            ),
            logs = listOf(
                StudyLog(
                    title = "Study",
                    contents = emptyList(),
                    completedContents = emptyList(),
                    studyingTime = 60_000L,
                    createAt = date.atTime(1, 1).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
                    startedAt = date.atTime(1, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli(),
                    id = "log-1",
                    isSelected = false
                )
            )
        )
    }
}
