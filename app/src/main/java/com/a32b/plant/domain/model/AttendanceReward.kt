package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

sealed interface AttendanceReward {
    data class Coin(val amount: Int) : AttendanceReward

    data class ItemReward(val type: ItemType, val amount: Int) : AttendanceReward
}
