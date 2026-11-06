package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingGoalDraft
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class SaveStudyingGoalDraftUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /** 목표 입력 창에서 작성 중인 글을 현재 유저·화분과 함께 로컬에 임시 저장 */
    suspend operator fun invoke(potId: String, logs: List<String>): Result<Unit> {
        return ensureCurrentUserUseCase {
            repository.saveLocalGoalDraft(StudyingGoalDraft(it.uid, potId, logs))
        } ?: Result.Failure(AppError.UnknownUser())
    }
}
