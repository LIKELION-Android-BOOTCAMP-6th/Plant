package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> =
        userRepository.updateDarkMode(isDarkMode)
}
