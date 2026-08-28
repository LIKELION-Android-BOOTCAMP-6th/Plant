package com.a32b.plant.domain.usecase.session

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import javax.inject.Inject

class EnsureCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val notifier: SessionExpiredNotifier
){
    operator fun invoke() : Result<User> {
        val user = userRepository.currentUser.value
        return if (user == null) {
            notifier.notifySessionExpired()
            Result.Failure(AppError.UnknownUser())
        }else{
            Result.Success(user)
        }
    }

    inline operator fun <T> invoke(block: (User) -> T): T? {
        val result = invoke()

        val user = when (result) {
            is Result.Success -> result.data
            is Result.Failure -> return null
        }
        return block(user)
    }
}
