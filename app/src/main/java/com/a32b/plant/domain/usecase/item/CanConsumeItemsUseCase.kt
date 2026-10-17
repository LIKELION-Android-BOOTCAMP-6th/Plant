package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CanConsumeItemsUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(items: Map<ItemType, Int>): Result<Boolean> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return itemRepository.canConsumeItems(user.uid, items)
    }
}
