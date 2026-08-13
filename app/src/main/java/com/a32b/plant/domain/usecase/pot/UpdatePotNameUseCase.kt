package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject

class UpdatePotNameUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    // 화분 이름 수정 (빈 값이 아닐 때만 수행)
    suspend operator fun invoke(uid: String, potId: String, newName: String) {
        if (newName.isNotBlank()) {
            potRepository.updatePotName(uid, potId, newName)
        }
    }
}