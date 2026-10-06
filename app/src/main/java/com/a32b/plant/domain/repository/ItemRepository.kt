package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType

interface ItemRepository {
    suspend fun addDrawItems(uid: String, items: List<ItemType>) : Result<Unit>
    suspend fun openBonusBoxes(uid: String, items: List<ItemType>) : Result<Unit>
}
