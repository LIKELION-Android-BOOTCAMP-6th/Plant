package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBoxCountUseCase @Inject constructor(
    private val repository: ItemRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    operator fun invoke(): Flow<Int> = flow {
        val result = ensureCurrentUserUseCase()
        if (result is Result.Success) {
            emitAll(repository.getBoxCount(result.data.uid))
        }
    }
}
