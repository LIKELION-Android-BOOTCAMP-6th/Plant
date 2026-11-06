package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class FinishStudyingUseCase @Inject constructor(
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(potId : String, timestamp: String, log: List<String>, completed: List<Boolean>, time: Long, startedAt: Long ) : Result<Unit> {
        return coroutineScope {
            val results = awaitAll(
                async { repository.saveStudyLog(potId, StudyLog.write(timestamp, log, completed, time, startedAt)) },
                async { repository.updateTotalStudyTime(potId, time) },
                async { repository.updateUserTotalStudyTime(time) },
            )
            results.firstOrNull { it is Result.Failure } ?: Result.Success(Unit)
        }
    }

    companion object {
        /** 학습 기록 저장 제한 시간 — 오프라인에서는 쓰기가 끝나지 않으므로 호출부가 이 시간만 기다림 */
        val SAVE_TIMEOUT = 5.seconds
    }
}
