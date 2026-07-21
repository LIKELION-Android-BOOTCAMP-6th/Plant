package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.User
import com.a32b.plant.origin.OldUserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMyPageProfileUseCase @Inject constructor(
    private val userRepository: OldUserRepository
) {
    operator fun invoke(): Flow<User?> {
        return userRepository.getUserProfile(CurrentUser.uid)
    }
}
