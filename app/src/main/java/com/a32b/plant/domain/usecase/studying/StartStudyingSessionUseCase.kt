package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class StartStudyingSessionUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val repository: StudyingRepository
) {
    private val currentUser = userRepository.currentUser
    suspend operator fun invoke(tag: String, title: String, potId: String, time: Long, log: List<String>?) : Result<Unit> {
        val user = currentUser.value ?: return Result.Failure(AppError.UnknownUser())
        val result = repository.initStudyingUser(
            StudyingUser(user.uid, user.nickname, user.profileImg, tag, time)
        )

        if (result is Result.Failure) return result

        return repository.saveLocalSession(
            StudyingSession(user.uid,tag, title, potId, time, log)
        )

    }
}
