package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class GetStudyingGoalDraftUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 목표 입력 창 임시 글 조회
     * 현재 유저·같은 화분의 글이고 작성한 내용이 있을 때만 반환, 아니면 null
     * (다른 계정이나 다른 화분에서 쓰던 글은 보여주지 않음)
     */
    suspend operator fun invoke(potId: String): Result<List<String>?> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.readLocalGoalDraft()
            .map { draft ->
                draft
                    ?.takeIf { it.userId == user.uid && it.potId == potId && it.logs.any { log -> log.isNotBlank() } }
                    ?.logs
            }
    }
}
