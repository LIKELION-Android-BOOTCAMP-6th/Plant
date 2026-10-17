package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class RecoverStudySessionUseCase @Inject constructor(
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(session: StudyingSession): Result<Unit> {
        val startedAtDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(session.startedAt),
            ZoneId.systemDefault()
        )
        val endedAtMillis = session.startedAt + session.time
        val timestamp = "${TimeFormatter.formatToKoreanDate(startedAtDateTime)} " +
                "${TimeFormatter.formatToTimeOnly(session.startedAt)} ~ " +
                TimeFormatter.formatToTimeOnly(endedAtMillis)

        val saveResult = finishStudyingUseCase(
            potId = session.potId,
            timestamp = timestamp,
            log = session.log ?: emptyList(),
            time = session.time,
            startedAt = session.startedAt
        )
        if (saveResult is Result.Failure) return saveResult

        repository.deleteStudyingUserInfo()
        return repository.clearLocalSession()
    }
}
