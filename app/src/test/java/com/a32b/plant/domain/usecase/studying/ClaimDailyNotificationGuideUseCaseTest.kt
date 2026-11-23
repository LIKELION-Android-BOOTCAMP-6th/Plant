package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.Result
import com.a32b.plant.fake.FakeStudyingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 학습 화면 알림 설정 안내를 하루 1번만 띄우는지 확인 (#610)
 */
class ClaimDailyNotificationGuideUseCaseTest {

    private val today = LocalDate.of(2026, 11, 23)
    private val repo = FakeStudyingRepository()
    private val useCase = ClaimDailyNotificationGuideUseCase(repo)

    @Test
    fun `shows and records today when never shown`() = runBlocking {
        assertTrue(useCase(today))
        assertEquals(listOf(today), repo.savedNotificationGuideShownDates)
    }

    @Test
    fun `shows and records today when last shown on another day`() = runBlocking {
        repo.notificationGuideShownDateResult = Result.Success(today.minusDays(1))

        assertTrue(useCase(today))
        assertEquals(listOf(today), repo.savedNotificationGuideShownDates)
    }

    @Test
    fun `does not show again on the same day`() = runBlocking {
        repo.notificationGuideShownDateResult = Result.Success(today)

        assertFalse(useCase(today))
        assertTrue(repo.savedNotificationGuideShownDates.isEmpty())
    }

    @Test
    fun `shows when reading the last date fails`() = runBlocking {
        repo.notificationGuideShownDateResult = Result.Failure(AppError.Local("불러오기 실패"))

        assertTrue(useCase(today))
        assertEquals(listOf(today), repo.savedNotificationGuideShownDates)
    }

    @Test
    fun `still shows when recording today fails`() = runBlocking {
        repo.saveNotificationGuideShownDateResult = Result.Failure(AppError.Local())

        assertTrue(useCase(today))
    }
}
