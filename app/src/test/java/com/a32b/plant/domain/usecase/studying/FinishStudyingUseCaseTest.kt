package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 학습 기록 저장 전 사전 검사 — 시작 시각 7일 하한 (#633)
 * 보안 규칙이 거부할 기록은 보내지 않고 거부로 돌려주는지 확인한다
 */
class FinishStudyingUseCaseTest {

    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    private suspend fun finish(repo: FakeStudyingRepository, startedAt: Long, time: Long = hour) =
        createFinishStudyingUseCase(repo)("pot-1", "session-1", "제목", listOf("목표"), listOf(true), time, startedAt)

    @Test
    fun `just finished session is sent (no regression)`() = runBlocking {
        val repo = FakeStudyingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - hour - minute)
        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.savedStudyLogs.size)
    }

    @Test
    fun `session started within 7 days is sent`() = runBlocking {
        val repo = FakeStudyingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - 7 * day + minute)
        assertEquals(Result.Success(StudySaveResult.Saved), result)
        assertEquals(1, repo.savedStudyLogs.size)
    }

    @Test
    fun `session started more than 7 days ago is rejected as TOO_OLD without sending`() = runBlocking {
        val repo = FakeStudyingRepository()
        val result = finish(repo, startedAt = System.currentTimeMillis() - 7 * day - minute)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.savedStudyLogs.isEmpty())
    }

    @Test
    fun `session started in 1970 is rejected as TOO_OLD without sending`() = runBlocking {
        val repo = FakeStudyingRepository()
        val result = finish(repo, startedAt = 0L)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.TOO_OLD)), result)
        assertTrue(repo.savedStudyLogs.isEmpty())
    }

    @Test
    fun `invalid time is still checked first`() = runBlocking {
        val repo = FakeStudyingRepository()
        val result = finish(repo, startedAt = 0L, time = 0L)
        assertEquals(Result.Success(StudySaveResult.Rejected(StudySaveRejection.INVALID_TIME)), result)
    }

    @Test
    fun `TOO_OLD message tells the same period as the constant`() {
        // 문구 전체는 고정하지 않는다 (문구 톤 통일·문자열 리소스 이동 #646에서 바뀔 수 있음)
        assertTrue(StudySaveRejection.TOO_OLD.message.contains("${StudyLog.MAX_OFFLINE_SYNC_DAYS}일"))
    }
}

/** 테스트용 [FinishStudyingUseCase] — 로그인한 유저(uid-1)가 있는 상태 */
internal fun createFinishStudyingUseCase(repo: FakeStudyingRepository) = FinishStudyingUseCase(
    repo,
    EnsureCurrentUserUseCase(
        FakeUserRepository(User.create().copy(uid = "uid-1")),
        object : SessionExpiredNotifier {
            override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
        }
    )
)
