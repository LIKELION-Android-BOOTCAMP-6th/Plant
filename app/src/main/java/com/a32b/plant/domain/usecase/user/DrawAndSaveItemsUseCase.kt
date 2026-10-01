package com.a32b.plant.domain.usecase.user

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.core.extension.drawItems
import javax.inject.Inject
import kotlin.random.Random

class DrawAndSaveItemsUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(uid: String, count: Int): Result<List<ItemType>>{
        if (count <= 0) return Result.Failure(AppError.Debug("아이템 count 오류"))

        val drawItems = Random.drawItems(count)

        return repository.addDrawItems(uid, drawItems).map { drawItems }
    }
}
