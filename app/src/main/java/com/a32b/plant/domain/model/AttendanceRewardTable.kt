package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

/**
 * 출석판 표시용 보상 표 (#531).
 * 원본은 서버(functions/index.js ATTENDANCE_REWARD_TABLE)이고, 실제 지급도 서버가 한다.
 * 서버 표를 바꾸면 이 표도 같이 바꿀 것 (다르면 출석판에 보이는 보상과 실제 지급이 어긋난다)
 */
object AttendanceRewardTable {
    fun of(count: Int): AttendanceReward? = when (count) {
        2 -> AttendanceReward.ItemReward(ItemType.HEART, 1)
        4 -> AttendanceReward.ItemReward(ItemType.SUN, 1)
        7 -> AttendanceReward.Coin(100)
        9 -> AttendanceReward.ItemReward(ItemType.WATER, 1)
        12 -> AttendanceReward.ItemReward(ItemType.HEART, 1)
        14 -> AttendanceReward.Coin(200)
        17 -> AttendanceReward.ItemReward(ItemType.SUN, 1)
        20 -> AttendanceReward.ItemReward(ItemType.FERTILIZER, 1)
        21 -> AttendanceReward.Coin(300)
        23 -> AttendanceReward.ItemReward(ItemType.WATER, 1)
        25 -> AttendanceReward.ItemReward(ItemType.NUTRIENT, 1)
        28 -> AttendanceReward.Coin(500)
        else -> null
    }
}
