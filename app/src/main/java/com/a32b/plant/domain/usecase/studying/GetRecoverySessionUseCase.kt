package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class GetRecoverySessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 복구 창에서 처리해야 할 로컬 백업 세션 조회
     * 현재 유저의 백업이고 쌓인 시간이 있을 때만 반환, 없으면 null
     */
    suspend operator fun invoke(): Result<StudyingSession?> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.readLocalSession()
            .map { session -> session?.takeIf { it.userId == user.uid && it.time > 0L } }
    }
}
