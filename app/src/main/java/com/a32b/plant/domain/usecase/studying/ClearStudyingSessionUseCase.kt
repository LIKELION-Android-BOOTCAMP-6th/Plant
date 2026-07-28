package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class ClearStudyingSessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
) {

    suspend operator fun invoke() : Result<Unit>{

        val result = repository.deleteStudyingUserInfo()

        if (result is Result.Failure) return result

        return repository.clearLocalSession()
    }
}
