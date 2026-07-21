package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUserBridge
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import javax.inject.Inject

/** 성공/실패와 무관하게 로그아웃되므로 Result를 사용하지 않는다. */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke() {
        authRepository.signOut()
        userRepository.clearCurrentUser()
        // TODO: CurrentUser 제거 대상 - 사유는 CurrentUserBridge의 KDoc 참고
        CurrentUserBridge.clear()
    }
}
