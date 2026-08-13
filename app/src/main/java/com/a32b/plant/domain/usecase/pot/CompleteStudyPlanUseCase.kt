package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject

class CompleteStudyPlanUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    // 화분 학습 완료 상태로 변경 및 완료 카운트 증가
    suspend operator fun invoke(uid: String, potId: String) {
        potRepository.completeStudyPlan(uid, potId)
    }
}