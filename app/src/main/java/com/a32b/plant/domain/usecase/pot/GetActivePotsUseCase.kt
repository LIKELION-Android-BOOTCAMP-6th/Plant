package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetActivePotsUseCase @Inject constructor(
    private val repository: PotRepository
) {
    //화분 변경 다이얼로그
    operator fun invoke(uid: String): Flow<List<Pot>> =
        repository.getPots(uid).map { pots ->
            pots.filter { !it.isCompleted }
        }
}