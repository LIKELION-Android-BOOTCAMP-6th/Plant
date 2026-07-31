package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class FinishStudyingUseCase @Inject constructor(
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(potId : String, timestamp: String, log: List<String>, time: Long ) : Result<Unit> {
        return coroutineScope {
            val results = awaitAll(
                async { repository.saveStudyLog(potId, StudyLog.write(timestamp, log, time)) },
                async { repository.updateTotalStudyTime(potId, time) },
                async { repository.updateUserTotalStudyTime(time) },
            )
            results.firstOrNull { it is Result.Failure } ?: Result.Success(Unit)
        }
    }
}
