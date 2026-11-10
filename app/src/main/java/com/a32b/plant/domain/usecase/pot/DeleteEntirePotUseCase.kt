package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class DeleteEntirePotUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 화분 전체 삭제 및 유저 누적 공부시간 차감 처리 (#455).
     * 차감량은 화면 값이 아니라 저장된 화분 값으로 정한다 (Repository). 하위 학습 기록은 서버가 지운다.
     * - 유저 없음: 세션 만료 처리 후 `AppError.UnknownUser` 실패 (호출부는 토스트를 띄우지 않는다)
     * - 이미 없는 화분이면 [SaveResult.Saved]
     * - 오프라인이면 [SaveResult.Pending] (기기에는 반영됨, 연결되면 전송)
     */
    suspend operator fun invoke(potId: String): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return potRepository.deleteEntirePot(user.uid, potId)
    }
}
