package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject

class GetPotDetailUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    // 특정 화분의 상세 정보를 조회 - 공부기록 화면에서 사용
    suspend operator fun invoke(uid: String, potId: String): Pot? =
        potRepository.getUserPotById(uid, potId)
}