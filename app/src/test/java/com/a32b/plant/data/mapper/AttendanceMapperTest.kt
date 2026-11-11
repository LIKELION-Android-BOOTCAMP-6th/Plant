package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.AttendanceResultDto
import com.a32b.plant.data.model.AttendanceRewardDto
import com.a32b.plant.data.model.DailyCheckThisMonthDto
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.type.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 출석 함수(functions/index.js checkAttendance) 응답 해석 (#531) */
class AttendanceMapperTest {

    @Test
    fun `success without reward`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 1, reward = null),
            AttendanceResultDto(status = "SUCCESS", newCount = 1, reward = null).toDomain()
        )
    }

    @Test
    fun `success with item reward`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 20, reward = AttendanceReward.ItemReward(ItemType.FERTILIZER, 1)),
            AttendanceResultDto(
                status = "SUCCESS",
                newCount = 20,
                reward = AttendanceRewardDto(item = "fertilizer", amount = 1)
            ).toDomain()
        )
    }

    @Test
    fun `success with gold reward`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 7, reward = AttendanceReward.Coin(100)),
            AttendanceResultDto(status = "SUCCESS", newCount = 7, reward = AttendanceRewardDto(gold = 100)).toDomain()
        )
    }

    @Test
    fun `already checked and month completed`() {
        assertEquals(AttendanceDecision.AlreadyChecked, AttendanceResultDto(status = "ALREADY_CHECKED").toDomain())
        assertEquals(AttendanceDecision.MonthCompleted, AttendanceResultDto(status = "MONTH_COMPLETED").toDomain())
    }

    // 지급은 서버에서 끝났으므로 출석은 성공으로 보여주고 보상 안내만 생략한다
    @Test
    fun `unknown reward item is shown as no reward`() {
        assertEquals(
            AttendanceDecision.Success(newCount = 2, reward = null),
            AttendanceResultDto(
                status = "SUCCESS",
                newCount = 2,
                reward = AttendanceRewardDto(item = "unknown", amount = 1)
            ).toDomain()
        )
    }

    // "gold"는 item 필드 키가 아니다 (골드는 gold 값으로만 온다)
    @Test
    fun `gold field key as item is not mapped to a gold ItemType`() {
        assertNull(
            (AttendanceResultDto(
                status = "SUCCESS",
                newCount = 2,
                reward = AttendanceRewardDto(item = "gold", amount = 1)
            ).toDomain() as AttendanceDecision.Success).reward
        )
    }

    @Test
    fun `unknown status or success without count cannot be interpreted`() {
        assertNull(AttendanceResultDto(status = "").toDomain())
        assertNull(AttendanceResultDto(status = "SOMETHING").toDomain())
        assertNull(AttendanceResultDto(status = "SUCCESS", newCount = null).toDomain())
    }

    @Test
    fun `lastCheckedDate is read from the user document`() {
        val domain = DailyCheckThisMonthDto(count = 3, lastCheckedDate = "2026-11-05").toDomain()
        assertEquals("2026-11-05", domain.lastCheckedDate)
        assertEquals(3, domain.count)
    }
}
