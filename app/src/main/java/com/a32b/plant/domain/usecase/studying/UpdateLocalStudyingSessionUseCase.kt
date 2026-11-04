package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class UpdateLocalStudyingSessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {

    suspend operator fun invoke(tag: String, title: String, potId: String, time: Long, log: List<String>?, startedAt: Long, isPaused: Boolean, status: StudyingSessionStatus = StudyingSessionStatus.STUDYING) : Result<Unit>{

        return ensureCurrentUserUseCase{
            repository.saveLocalSession(
                StudyingSession(it.uid, tag, title, potId, time,log, startedAt, isPaused, status)
            )
        } ?: Result.Failure(AppError.UnknownUser())
    }

}
