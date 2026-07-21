package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUserBridge
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class SetNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String, nickname: String): Result<Unit> {
        val duplicateCheck = userRepository.isNicknameTaken(nickname)
        if (duplicateCheck is Result.Failure) return duplicateCheck
        if ((duplicateCheck as Result.Success).data) {
            return Result.Failure(AppError.Custom("사용 중인 닉네임입니다."))
        }

        val registerResult = userRepository.registerNickname(nickname)
        if (registerResult is Result.Failure) return registerResult

        val completeResult = userRepository.completeFirstLogin(uid, nickname)
        if (completeResult is Result.Failure) return completeResult

        userRepository.currentUser.value?.let { current ->
            val updated = current.copy(nickname = nickname, isFirstLogin = false)
            userRepository.setCurrentUser(updated)
            // TODO: CurrentUser 제거 대상 - 사유는 CurrentUserBridge의 KDoc 참고
            CurrentUserBridge.set(uid, updated.nickname, updated.profileImg)
        }

        return Result.Success(Unit)
    }
}
