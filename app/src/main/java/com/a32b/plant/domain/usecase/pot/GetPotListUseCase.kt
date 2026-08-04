package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetPotListUseCase @Inject constructor(
    private val repository: PotRepository
) {
    //전체 가져오기
    operator fun invoke(uid: String): Flow<List<Pot>> = repository.getPots(uid)

    // 공부 중인 화분만
    fun getStudyingPots(uid: String): Flow<List<Pot>> =
        repository.getPots(uid).map { pots -> pots.filter { !it.isCompleted } }

    // 공부 완료된 화분만
    fun getCompletedPots(uid: String): Flow<List<Pot>> =
        repository.getPots(uid).map { pots -> pots.filter { it.isCompleted } }
}