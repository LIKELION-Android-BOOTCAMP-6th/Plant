package com.a32b.plant.data.source.remote.item

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRemoteDataSource {
    suspend fun openBonusBoxes(boxCount: Int, requestId: String): BonusBoxOpeningDto
    fun observeBoxCount(uid: String): Flow<Int>
}
