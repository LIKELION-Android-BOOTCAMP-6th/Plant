package com.a32b.plant.data.source.remote.item

import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRemoteDataSource {
    suspend fun addDrawItems(uid: String, items: List<ItemType>)
    suspend fun openBonusBoxes(uid: String, items: List<ItemType>)
    fun observeBoxCount(uid: String): Flow<Int>
    suspend fun consumeItems(uid: String, items: Map<ItemType, Int>)
    suspend fun canConsumeItems(uid: String, items: Map<ItemType, Int>): Boolean
}
