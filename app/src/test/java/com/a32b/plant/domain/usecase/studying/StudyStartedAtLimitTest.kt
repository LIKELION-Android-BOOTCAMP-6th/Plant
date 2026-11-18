package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 학습 기록 시작 시각 7일 하한의 앱 사전 검사 (#633)
 * 보안 규칙이 거부할 기록은 학습 종료([FinishStudyingUseCase])와 홈 복구([RecoverStudySessionUseCase]) 모두 보내지 않고 거부한다
 */
class StudyStartedAtLimitTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    /** 보낸 저장 요청과 백업 삭제 횟수를 기록하는 저장소. 나머지는 공용 Fake(호출되면 실패)에 맡긴다 */
    private class RecordingRepository(
        private val base: FakeStudyingRepository = FakeStudyingRepository()
    ) : StudyingRepository by base {
        val sentLogs = mutableListOf<StudyLog>()
        val savedBackups = mutableListOf<StudyingSession>()
        val clearCalls get() = base.clearLocalSessionCalls

        override suspend fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLog): Result<StudySaveResult> {
            sentLogs += log
            return Result.Success(StudySaveResult.Saved)
        }

        override suspend fun saveLocalSession(studying: StudyingSession): Result<Unit> {
            savedBackups += studying
            return Result.Success(Unit)
        }
    }

    private fun finishUseCase(repo: StudyingRepository) = FinishStudyingUseCase(
        repo,
        EnsureCurrentUserUseCase(
            FakeUserRepository(User.create().copy(uid = "uid-1")),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
    )

    private suspend fun finish(repo: StudyingRepository, startedAt: Long, time: Long = hour) =
        finishUseCase(repo)("pot-1", "session-1", "제목", listOf("목표"), listOf(true), time, startedAt)

    // ---- FinishStudyingUseCase (학습 화면 종료) ----

    @Test
    fun `just finished session is sent (no regression)`() = runBlocking {
        val repo = RecordingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - hour - minute)
        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.sentLogs.size)
    }

    @Test
    fun `session started within 7 days is sent`() = runBlocking {
        val repo = RecordingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - 7 * day + minute)
        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.sentLogs.size)
    }

    @Test
    fun `session started more than 7 days ago is rejected as TOO_OLD without sending`() = runBlocking {
        val repo = RecordingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - 7 * day - minute)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.sentLogs.isEmpty())
    }

    @Test
    fun `session started in 1970 is rejected as TOO_OLD without sending`() = runBlocking {
        val repo = RecordingRepository()
        val result = finish(repo, startedAt = 0L)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.sentLogs.isEmpty())
    }

    @Test
    fun `invalid time is still checked first`() = runBlocking {
        val repo = RecordingRepository()
        val result = finish(repo, startedAt = 0L, time = 0L)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.INVALID_TIME)), result)
    }

    @Test
    fun `TOO_OLD message says 7 days`() {
        assertEquals("학습을 시작한 지 7일이 지나 기록을 저장할 수 없어요.", StudySaveRejection.TOO_OLD.message)
    }

    // ---- RecoverStudySessionUseCase (홈 복구 창) ----

    private fun backup(startedAt: Long, sessionId: String = "session-1") = StudyingSession(
        userId = "uid-1", tag = "태그", title = "화분", potId = "pot-1", time = hour,
        log = listOf("목표"), startedAt = startedAt, isPaused = false,
        status = StudyingSessionStatus.FINISHED, sessionId = sessionId, completed = listOf(true)
    )

    @Test
    fun `recovering backup older than 7 days returns TOO_OLD and clears backup without sending`() = runBlocking {
        val repo = RecordingRepository()
        val useCase = RecoverStudySessionUseCase(finishUseCase(repo), repo)

        val result = useCase(backup(startedAt = System.currentTimeMillis() - 8 * day))

        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.sentLogs.isEmpty())
        assertEquals(1, repo.clearCalls)
    }

    @Test
    fun `recovering old backup from previous version (no session id) also returns TOO_OLD`() = runBlocking {
        val repo = RecordingRepository()
        val useCase = RecoverStudySessionUseCase(finishUseCase(repo), repo)

        val result = useCase(backup(startedAt = System.currentTimeMillis() - 8 * day, sessionId = ""))

        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertEquals(1, repo.savedBackups.size) // 새 세션 ID를 백업에 먼저 저장하는 기존 동작은 그대로
        assertTrue(repo.sentLogs.isEmpty())
        assertEquals(1, repo.clearCalls)
    }

    @Test
    fun `recovering backup within 7 days is sent and cleared (no regression)`() = runBlocking {
        val repo = RecordingRepository()
        val useCase = RecoverStudySessionUseCase(finishUseCase(repo), repo)

        val result = useCase(backup(startedAt = System.currentTimeMillis() - 2 * day))

        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.sentLogs.size)
        assertEquals(1, repo.clearCalls)
    }
}
