package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

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
