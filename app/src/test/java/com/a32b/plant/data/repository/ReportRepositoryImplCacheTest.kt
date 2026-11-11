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
import com.google.android.gms.tasks.Task
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

    @Test
    fun `multiple pots are server based when every query is server based`() = runTest {
        val repository = multiPotRepository(false, false, false)
        assertFalse(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    @Test
    fun `multiple pots are cache based when first pot query is cached`() = runTest {
        val repository = multiPotRepository(false, true, false)
        assertTrue(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    @Test
    fun `multiple pots are cache based when last pot query is cached`() = runTest {
        val repository = multiPotRepository(false, false, true)
        assertTrue(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    @Test
    fun `multiple server pot queries are cache based when pot list is cached`() = runTest {
        val repository = multiPotRepository(true, false, false)
        assertTrue(repository.getPeriodData("uid", 0L, 1L).getOrThrow().isFromCache)
    }

    private fun multiPotRepository(
        potListFromCache: Boolean,
        firstPotFromCache: Boolean,
        secondPotFromCache: Boolean
    ) = ReportRepositoryImpl(
        potRemoteDataSource = FakePotRemoteDataSource(
            GetPotsOnceResult(
                listOf(PotDto(id = "pot-1"), PotDto(id = "pot-2")),
                isFromCache = potListFromCache
            )
        ),
        studyLogRemoteDataSource = FakeStudyLogRemoteDataSource(
            resultsByPotId = mapOf(
                "pot-1" to GetPotLogsInPeriodResult(emptyList(), isFromCache = firstPotFromCache),
                "pot-2" to GetPotLogsInPeriodResult(emptyList(), isFromCache = secondPotFromCache)
            )
        )
    )

    private class FakePotRemoteDataSource(private val result: GetPotsOnceResult) : PotRemoteDataSource {
        override fun getPots(uid: String): Flow<List<PotDto>> = emptyFlow()
        override suspend fun addPot(uid: String, tag: Tag, name: String): kotlin.Result<Unit> = error("Unused")
        override suspend fun getPotsOnce(uid: String): GetPotsOnceResult = result
        override fun updatePotName(uid: String, potId: String, name: String): Task<Void> = error("Unused")
        override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): List<StudyLogDto> =
            error("Unused")
        override fun levelUpPot(
            uid: String,
            potId: String,
            newLevel: Int,
            items: Map<ItemType, Int>
        ): Task<Void> = error("Unused")
        override suspend fun getPotPreferringCache(uid: String, potId: String): PotDto? = error("Unused")
        override suspend fun getPotOnServer(uid: String, potId: String): PotDto? = error("Unused")
        override fun completePot(uid: String, potId: String): Task<Void> = error("Unused")
        override fun deletePot(
            uid: String,
            potId: String,
            potTotalStudyingTime: Long,
            isCompleted: Boolean
        ): Task<Void> = error("Unused")
    }

    private class FakeStudyLogRemoteDataSource(
        private val result: GetPotLogsInPeriodResult? = null,
        private val resultsByPotId: Map<String, GetPotLogsInPeriodResult> = emptyMap()
    ) : StudyLogRemoteDataSource {
        override fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLogDto>> = emptyFlow()
        override suspend fun getPotLogs(uid: String, potId: String): List<StudyLogDto> = error("Unused")
        override suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLogDto? =
            error("Unused")
        override suspend fun getStudyLogPreferringCache(uid: String, potId: String, logId: String): StudyLogDto? =
            error("Unused")
        override suspend fun existsStudyLogOnServer(uid: String, potId: String, logId: String): Boolean =
            error("Unused")
        override fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long): Task<Void> =
            error("Unused")
        override suspend fun getPotLogsInPeriod(
            uid: String,
            potId: String,
            startInclusive: Timestamp,
            endExclusive: Timestamp
        ): GetPotLogsInPeriodResult = resultsByPotId[potId] ?: result ?: error("Unexpected potId: $potId")
    }

    private fun <T> Result<T>.getOrThrow(): T = when (this) {
        is Result.Success -> data
        is Result.Failure -> throw AssertionError("Expected success, got $error")
    }
}
