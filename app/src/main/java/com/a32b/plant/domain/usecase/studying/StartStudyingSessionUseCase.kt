package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class StartStudyingSessionUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(tag: String, title: String, potId: String, time: Long, log: List<String>?) : Result<Unit> {
        return ensureCurrentUserUseCase {
            val result = repository.initStudyingUser(
                StudyingUser(it.uid, it.nickname, it.profileImg, tag, time)
            )
            if (result is Result.Failure) {
                result
            } else {
                repository.saveLocalSession(
                    StudyingSession(it.uid, tag, title, potId, time, log)
                )
            }
        } ?: Result.Failure(AppError.UnknownUser())
    }
}
