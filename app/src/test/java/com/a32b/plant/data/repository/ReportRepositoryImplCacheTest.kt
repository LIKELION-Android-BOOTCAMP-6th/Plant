package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.StudyLogRemoteDataSource
import com.a32b.plant.data.datasource.GetPotLogsInPeriodResult
import com.a32b.plant.data.model.PotDto
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.source.remote.pot.GetPotsOnceResult
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.model.Tag
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ReportRepositoryImplCacheTest {
    @Test
    fun `period source is cache based when any query is cached`() = runTest {
        val repository = ReportRepositoryImpl(
            potRemoteDataSource = FakePotRemoteDataSource(
                GetPotsOnceResult(listOf(PotDto(id = "pot-1")), isFromCache = false)
            ),
            studyLogRemoteDataSource = FakeStudyLogRemoteDataSource(
                GetPotLogsInPeriodResult(emptyList(), isFromCache = true)
            )
        )

        assertTrue(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    @Test
    fun `period source is server based when every query is server based`() = runTest {
        val repository = ReportRepositoryImpl(
            potRemoteDataSource = FakePotRemoteDataSource(
                GetPotsOnceResult(listOf(PotDto(id = "pot-1")), isFromCache = false)
            ),
            studyLogRemoteDataSource = FakeStudyLogRemoteDataSource(
                GetPotLogsInPeriodResult(emptyList(), isFromCache = false)
            )
        )

        assertFalse(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    @Test
    fun `cached pot list marks empty period as cache based`() = runTest {
        val repository = ReportRepositoryImpl(
            potRemoteDataSource = FakePotRemoteDataSource(GetPotsOnceResult(emptyList(), isFromCache = true)),
            studyLogRemoteDataSource = FakeStudyLogRemoteDataSource(
                GetPotLogsInPeriodResult(emptyList(), isFromCache = false)
            )
        )

        assertTrue(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    private class FakePotRemoteDataSource(private val result: GetPotsOnceResult) : PotRemoteDataSource {
        override fun getPots(uid: String): Flow<List<PotDto>> = emptyFlow()
        override suspend fun addPot(uid: String, tag: Tag, name: String): kotlin.Result<Unit> = error("Unused")
        override suspend fun getPotsOnce(uid: String): GetPotsOnceResult = result
        override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): List<StudyLogDto> =
            error("Unused")
        override suspend fun levelUpWithItemConsumption(
            uid: String,
            potId: String,
            newLevel: Int,
            items: Map<ItemType, Int>
        ) = Unit
    }

    private class FakeStudyLogRemoteDataSource(private val result: GetPotLogsInPeriodResult) : StudyLogRemoteDataSource {
        override fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLogDto>> = emptyFlow()
        override suspend fun getPotLogs(uid: String, potId: String): List<StudyLogDto> = error("Unused")
        override suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLogDto? =
            error("Unused")
        override suspend fun executeDeleteBatch(uid: String, potId: String, logId: String, decreaseAmount: Long) =
            error("Unused")
        override suspend fun getPotLogsInPeriod(
            uid: String,
            potId: String,
            startInclusive: Timestamp,
            endExclusive: Timestamp
        ): GetPotLogsInPeriodResult = result
    }

    private fun <T> Result<T>.getOrThrow(): T = when (this) {
        is Result.Success -> data
        is Result.Failure -> throw AssertionError("Expected success, got $error")
    }
}
