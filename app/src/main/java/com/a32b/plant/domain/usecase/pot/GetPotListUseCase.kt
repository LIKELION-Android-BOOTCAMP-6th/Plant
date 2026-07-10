package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPotListUseCase @Inject constructor(
    private val repository: PotRepository
) {
    operator fun invoke(uid: String): Flow<List<Pot>> = repository.getPots(uid)
}