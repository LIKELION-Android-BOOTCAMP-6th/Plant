package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CompleteStudyPlanUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 화분 학습 완료 상태로 변경 및 완료 카운트 증가 (#455).
     * - 유저 없음: 세션 만료 처리 후 `AppError.UnknownUser` 실패 (호출부는 토스트를 띄우지 않는다)
     * - 이미 완료된 화분이면 보내지 않고 [SaveResult.Saved]
     * - 오프라인이면 [SaveResult.Pending] (기기에는 반영됨, 연결되면 전송)
     */
    suspend operator fun invoke(potId: String): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return potRepository.completeStudyPlan(user.uid, potId)
    }
}
