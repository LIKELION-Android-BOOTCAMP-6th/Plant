package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeStudyingRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 타이머 서비스의 주기 저장 — 로그인한 유저가 있을 때만 백업 시간을 갱신하는지 확인 (#668)
 * 백그라운드에서 세션이 만료된 뒤(유저 없음)에는 시간을 더 쌓지 않아야 한다
 */
class UpdateLocalStudyingTimeUseCaseTest {

    private val repo = FakeStudyingRepository()
    private val notifiedReasons = mutableListOf<SessionExpiredReason>()

    private fun createUseCase(user: User?) = UpdateLocalStudyingTimeUseCase(
        repo,
        EnsureCurrentUserUseCase(
            FakeUserRepository(user),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) {
                    notifiedReasons += reason
                }
            }
        )
    )

    @Test
    fun `updates time when user is logged in`() = runBlocking {
        val result = createUseCase(User.create().copy(uid = "uid-1"))("session-1", 30_000L)

        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf("session-1" to 30_000L), repo.updatedLocalSessionTimes)
        assertTrue(notifiedReasons.isEmpty())
    }

    @Test
    fun `does not update time and notifies session expired when user is missing`() = runBlocking {
        val result = createUseCase(user = null)("session-1", 30_000L)

        assertTrue(result is Result.Failure && result.error is AppError.UnknownUser)
        assertTrue(repo.updatedLocalSessionTimes.isEmpty())
        assertEquals(1, notifiedReasons.size)
    }
}
