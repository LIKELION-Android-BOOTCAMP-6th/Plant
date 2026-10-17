package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class OpenBonusBoxUseCase @Inject constructor(
    private val drawAndSaveItemsUseCase: DrawAndSaveItemsUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(): Result<List<ItemType>> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return drawAndSaveItemsUseCase(user.uid, 1, isBoxOpened = true)
    }
}
