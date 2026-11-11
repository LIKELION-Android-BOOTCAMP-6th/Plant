package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * 출석판 표시용 판단 (#531). 서버 decideAttendance(functions/index.js)와 같은 기준인지 확인한다.
 * 기준은 lastCheckedDate("yyyy-MM-dd")이고 lastCheckedAt(Timestamp)은 보지 않는다.
 */
class DailyCheckThisMonthTest {

    private val today = LocalDate.of(2026, 11, 5)

    private fun check(count: Int, lastCheckedDate: String?, lastCheckedAt: Long? = null) =
        DailyCheckThisMonth(count = count, lastCheckedAt = lastCheckedAt, lastCheckedDate = lastCheckedDate)

    @Test
    fun `never checked - first attendance of the month`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 1, reward = null),
            check(count = 0, lastCheckedDate = null).decideNext(today)
        )
    }

    @Test
    fun `checked yesterday in the same month - next count with reward`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 2, reward = AttendanceReward.ItemReward(ItemType.HEART, 1)),
            check(count = 1, lastCheckedDate = "2026-11-04").decideNext(today)
        )
    }

    @Test
    fun `already checked today`() {
        assertEquals(
            AttendanceDecision.AlreadyChecked,
            check(count = 3, lastCheckedDate = "2026-11-05").decideNext(today)
        )
    }

    @Test
    fun `28 checks this month - month completed even if last check was today`() {
        assertEquals(AttendanceDecision.MonthCompleted, check(28, "2026-11-04").decideNext(today))
        assertEquals(AttendanceDecision.MonthCompleted, check(28, "2026-11-05").decideNext(today))
    }

    @Test
    fun `last check in previous month - count resets to first attendance`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 1, reward = null),
            check(count = 28, lastCheckedDate = "2026-10-31").decideNext(today)
        )
    }

    @Test
    fun `same day of a different year is not the same month`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 1, reward = null),
            check(count = 5, lastCheckedDate = "2025-11-05").decideNext(today)
        )
    }

    // lastCheckedAt만 있고 lastCheckedDate가 없는 문서는 출석한 적 없는 것으로 표시한다
    // (배포 전 데이터 초기화 예정이라 예전 문서 변환은 하지 않음)
    @Test
    fun `lastCheckedAt alone is not used for the decision`() {
        val todayMillis = 1_793_836_800_000L // 2026-11-05 00:00 UTC = 09:00 KST
        assertEquals(
            AttendanceDecision.Success(newCount = 1, reward = null),
            check(count = 3, lastCheckedDate = null, lastCheckedAt = todayMillis).decideNext(today)
        )
    }
}
