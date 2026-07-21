package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUserBridge
import com.a32b.plant.domain.model.AutoLoginResult
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class CheckAutoLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<AutoLoginResult> {
        val uid = authRepository.currentUid()
            ?: return Result.Success(AutoLoginResult.NotLoggedIn)

        val getUserResult = userRepository.getUser(uid)
        if (getUserResult is Result.Failure) return getUserResult
        val user = (getUserResult as Result.Success).data

        // 세션은 있는데 Firestore 프로필이 없거나, 닉네임 미설정 상태면 로그인 화면으로 되돌린다.
        if (user == null || user.isFirstLogin == true) {
            authRepository.signOut()
            return Result.Success(AutoLoginResult.NotLoggedIn)
        }

        userRepository.setCurrentUser(user)
        // TODO: CurrentUser 제거 대상 - 사유는 CurrentUserBridge의 KDoc 참고
        CurrentUserBridge.set(uid, user.nickname, user.profileImg)

        return Result.Success(AutoLoginResult.LoggedIn(uid, user))
    }
}
