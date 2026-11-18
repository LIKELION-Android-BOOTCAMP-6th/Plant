package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.fake.FakeStudyingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 홈 복구 창의 백업 저장 — 시작 시각 7일 하한 (#633)
 * 복구도 [FinishStudyingUseCase]를 거치므로 오래된 백업은 보내지 않고 거부한 뒤 백업을 지우는지 확인한다
 */
class RecoverStudySessionUseCaseTest {

    private val hour = 60 * 60_000L
    private val day = 24 * hour

    private fun createUseCase(repo: FakeStudyingRepository) =
        RecoverStudySessionUseCase(createFinishStudyingUseCase(repo), repo)

    private fun backup(startedAt: Long, sessionId: String = "session-1") = StudyingSession(
        userId = "uid-1", tag = "태그", title = "화분", potId = "pot-1", time = hour,
        log = listOf("목표"), startedAt = startedAt, isPaused = false,
        status = StudyingSessionStatus.FINISHED, sessionId = sessionId, completed = listOf(true)
    )

    @Test
    fun `backup older than 7 days returns TOO_OLD and clears backup without sending`() = runBlocking {
        val repo = FakeStudyingRepository()

        val result = createUseCase(repo)(backup(startedAt = System.currentTimeMillis() - 8 * day))

        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.savedStudyLogs.isEmpty())
        assertEquals(1, repo.clearLocalSessionCalls)
    }

    @Test
    fun `old backup from previous version (no session id) also returns TOO_OLD`() = runBlocking {
        val repo = FakeStudyingRepository()

        val result = createUseCase(repo)(backup(startedAt = System.currentTimeMillis() - 8 * day, sessionId = ""))

        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertEquals(1, repo.savedLocalSessions.size) // 새 세션 ID를 백업에 먼저 저장하는 기존 동작은 그대로
        assertTrue(repo.savedStudyLogs.isEmpty())
        assertEquals(1, repo.clearLocalSessionCalls)
    }

    @Test
    fun `backup within 7 days is sent and cleared (no regression)`() = runBlocking {
        val repo = FakeStudyingRepository()

        val result = createUseCase(repo)(backup(startedAt = System.currentTimeMillis() - 2 * day))

        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.savedStudyLogs.size)
        assertEquals(1, repo.clearLocalSessionCalls)
    }
}
