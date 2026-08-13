package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> {
        return when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> {
                userRepository.updateDarkMode(
                    result.data.copy(isDarkMode = isDarkMode)
                )
            }

            is Result.Failure -> result
        }
    }
}
