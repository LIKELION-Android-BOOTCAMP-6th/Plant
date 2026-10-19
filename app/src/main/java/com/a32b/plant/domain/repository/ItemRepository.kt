package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    suspend fun addDrawItems(uid: String, items: List<ItemType>) : Result<Unit>
    suspend fun openBonusBoxes(uid: String, items: List<ItemType>) : Result<Unit>
    fun getBoxCount(uid: String): Flow<Int>
    suspend fun consumeItems(uid: String, items: Map<ItemType, Int>): Result<Unit>
    suspend fun canConsumeItems(uid: String, items: Map<ItemType, Int>): Result<Boolean>
}
