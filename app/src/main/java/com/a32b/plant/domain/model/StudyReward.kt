package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

data class StudyReward(
    val items: List<ItemType>,
    val boxCount: Int
)
