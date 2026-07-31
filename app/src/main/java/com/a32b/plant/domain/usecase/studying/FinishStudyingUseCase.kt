package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class FinishStudyingUseCase @Inject constructor(
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(potId : String, timestamp: String, log: List<String>, time: Long ) : Result<Unit>{

        val logResult = repository.saveStudyLog(potId, StudyLog.write(timestamp, log, time))
        if (logResult is Result.Failure && logResult.error !is AppError.Network) return logResult

        val potTimeResult = repository.updateTotalStudyTime(potId, time)
        if (potTimeResult is Result.Failure && potTimeResult.error !is AppError.Network) return potTimeResult

        val userTimeResult = repository.updateUserTotalStudyTime(time)
        if (userTimeResult is Result.Failure) return userTimeResult

        return Result.Success(Unit)

    }
}
