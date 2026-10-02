package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.core.extension.drawItems
import javax.inject.Inject
import kotlin.random.Random

class DrawAndSaveItemsUseCase @Inject constructor(
    private val repository: ItemRepository
) {
    suspend operator fun invoke(uid: String, count: Int, isBoxOpened: Boolean = false): Result<List<ItemType>>{
        if (count <= 0) return Result.Failure(AppError.Debug("아이템 count 오류"))

        val drawItems = Random.drawItems(count)

        val result = if (isBoxOpened) {
            repository.openBonusBoxes(uid, drawItems)
        } else {
            repository.addDrawItems(uid, drawItems)
        }

        return result.map { drawItems }
    }
}
