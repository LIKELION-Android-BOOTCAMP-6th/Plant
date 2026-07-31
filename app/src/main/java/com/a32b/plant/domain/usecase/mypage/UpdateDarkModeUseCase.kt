package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> {
        val user = userRepository.currentUser.value
            ?: return Result.Failure(AppError.Auth("로그인 정보가 없습니다."))

        if (user.uid.isBlank()) {
            return Result.Failure(AppError.Auth("로그인 정보가 없습니다."))
        }

        return userRepository.updateDarkMode(
            user.copy(isDarkMode = isDarkMode)
        )
    }
}
