package com.a32b.plant.data.source.remote.item

import com.a32b.plant.domain.type.ItemType

interface ItemRemoteDataSource {
    suspend fun addDrawItems(uid: String, items: List<ItemType>)
    suspend fun openBonusBoxes(uid: String, items: List<ItemType>)
}
