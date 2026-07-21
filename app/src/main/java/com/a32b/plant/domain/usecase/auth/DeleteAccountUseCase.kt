package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUserBridge
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        val uid = authRepository.currentUid()
            ?: return Result.Failure(AppError.Auth("로그인 정보가 없습니다."))
        val nickname = userRepository.currentUser.value?.nickname.orEmpty()

        // 1. Firestore 데이터 먼저 삭제 (인증 세션이 살아있는 동안)
        val deleteDataResult = userRepository.deleteUserData(uid)
        if (deleteDataResult is Result.Failure) return deleteDataResult

        if (nickname.isNotBlank()) {
            userRepository.deleteNickname(nickname)
        }

        // 2. Firebase Auth 계정은 맨 마지막에 삭제
        val deleteAuthResult = authRepository.deleteAuthAccount()
        if (deleteAuthResult is Result.Failure) return deleteAuthResult

        userRepository.clearCurrentUser()
        // TODO: CurrentUser 제거 대상 - 사유는 CurrentUserBridge의 KDoc 참고
        CurrentUserBridge.clear()

        return Result.Success(Unit)
    }
}
