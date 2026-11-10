package com.a32b.plant.domain.usecase.studyLog

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.StudyLogRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class DeleteStudyLogUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 특정 학습 기록 삭제 및 연관된 공부 시간 차감 (#455).
     * 차감량은 화면 값이 아니라 저장된 기록 값으로 정한다 (Repository).
     * - 유저 없음: 세션 만료 처리 후 `AppError.UnknownUser` 실패 (호출부는 토스트를 띄우지 않는다)
     * - 이미 없는 기록이면 [SaveResult.Saved]
     * - 오프라인이면 [SaveResult.Pending] (기기에는 반영됨, 연결되면 전송)
     */
    suspend operator fun invoke(potId: String, logId: String): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return studyLogRepository.deleteStudyLog(user.uid, potId, logId)
    }
}
