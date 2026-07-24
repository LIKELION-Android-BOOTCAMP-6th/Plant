package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.origin.OldUserRepository
import javax.inject.Inject

class UpdateDarkModeUseCase @Inject constructor(
    // TODO: UserRepository의 다크모드 갱신 API가 확정되면
    // OldUserRepository, CurrentUser 의존성을 제거한다.
    private val userRepository: OldUserRepository
) {
    suspend operator fun invoke(isDarkMode: Boolean): Result<Unit> = runCatching {
        userRepository.updateIsDarkMode(
                uid = CurrentUser.uid,
                state = isDarkMode
        )
    }
}