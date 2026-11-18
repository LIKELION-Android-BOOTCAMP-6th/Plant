package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProfileImageLevelListUseCaseTest {
    @Test
    fun `returns all levels through the stored unlocked level`() = runTest {
        val result = createUseCase(testUser(3))()

        assertTrue(result is Result.Success)
        assertEquals(listOf(0, 1, 2, 3), (result as Result.Success).data)
    }

    @Test
    fun `returns level zero when no levels are unlocked`() = runTest {
        val result = createUseCase(testUser(0))()

        assertTrue(result is Result.Success)
        assertEquals(listOf(0), (result as Result.Success).data)
    }

    @Test
    fun `clamps a negative unlocked level to zero`() = runTest {
        val result = createUseCase(testUser(-1))()

        assertTrue(result is Result.Success)
        assertEquals(listOf(0), (result as Result.Success).data)
    }

    @Test
    fun `clamps an unlocked level above the max level to the max level`() = runTest {
        val result = createUseCase(testUser(PotLevelConditions.MAX_LEVEL + 1))()

        assertTrue(result is Result.Success)
        assertEquals((0..PotLevelConditions.MAX_LEVEL).toList(), (result as Result.Success).data)
    }

    @Test
    fun `returns unknown user failure and notifies when no user exists`() = runTest {
        val notifier = RecordingSessionExpiredNotifier()
        val result = createUseCase(user = null, notifier = notifier)()

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.UnknownUser)
        assertEquals(1, notifier.notificationCount)
    }

    private fun createUseCase(
        user: User?,
        notifier: RecordingSessionExpiredNotifier = RecordingSessionExpiredNotifier()
    ) = GetProfileImageLevelListUseCase(
        EnsureCurrentUserUseCase(FakeUserRepository(user), notifier)
    )

    private fun testUser(unlockedProfileLevel: Int) = User.create().copy(
        uid = "uid-1",
        unlockedProfileLevel = unlockedProfileLevel
    )

    private class RecordingSessionExpiredNotifier : SessionExpiredNotifier {
        var notificationCount = 0

        override fun notifySessionExpired(reason: SessionExpiredReason) {
            notificationCount++
        }
    }
}
