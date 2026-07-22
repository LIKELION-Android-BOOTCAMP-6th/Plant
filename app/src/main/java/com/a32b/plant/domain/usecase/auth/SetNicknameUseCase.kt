package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
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
            // TODO: CurrentUser 전역 싱글톤 제거 대상.
            //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 세팅한다.
            //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
            CurrentUser.set(UserModel(uid, updated.nickname, updated.profileImg))
        }

        return Result.Success(Unit)
    }
}
