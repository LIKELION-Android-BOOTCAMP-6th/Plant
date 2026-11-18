package com.a32b.plant.data.source.remote.item

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType

interface ItemRemoteDataSource {
    suspend fun openBonusBoxes(boxCount: Int, requestId: String): BonusBoxOpeningDto
}
