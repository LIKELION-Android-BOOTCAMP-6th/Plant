package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetActivePotUseCase @Inject constructor(
    private val repository: PotRepository
) {
    // 현재 메인 표출 화분을 반환
    operator fun invoke(uid: String, lastSelectedPotId: String): Flow<Pot> =
        repository.getPots(uid).map { pots ->
            pots.find { it.id == lastSelectedPotId } ?: pots.firstOrNull() ?: Pot.EMPTY
        }
}