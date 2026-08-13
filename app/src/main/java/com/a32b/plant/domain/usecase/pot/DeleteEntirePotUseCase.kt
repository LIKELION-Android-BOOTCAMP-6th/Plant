package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject

class DeleteEntirePotUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    // 화분 전체 삭제 및 유저 누적 공부시간 차감 처리
    suspend operator fun invoke(uid: String, potId: String, totalStudyingTime: Long) {
        potRepository.deleteEntirePot(uid, potId, totalStudyingTime)
    }
}