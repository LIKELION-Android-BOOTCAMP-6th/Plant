package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.origin.OldUserRepository
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
        private val userRepository: OldUserRepository
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> = runCatching {
        userRepository.updateIsDarkMode(
                uid = CurrentUser.uid,
                state = isDarkMode
        )
    }
}