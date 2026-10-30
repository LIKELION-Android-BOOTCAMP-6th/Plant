package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.core.util.SessionTokenGenerator
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
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

        val getUserResult = userRepository.getUserPreferringServer(uid)
        if (getUserResult is Result.Failure) return getUserResult
        val snapshot = (getUserResult as Result.Success).data
        val user = snapshot.user

        // 세션은 있는데 Firestore 프로필이 없거나, 닉네임 미설정 상태면 로그인 화면으로 되돌린다.
        if (user == null || user.isFirstLogin == true) {
            authRepository.signOut()
            return Result.Success(AutoLoginResult.NotLoggedIn)
        }

        val activeToken = if (snapshot.isFromServer) {
            val token = SessionTokenGenerator.generate()
            val tokenResult = userRepository.updateSessionToken(uid, token)
            // 실패해도 로그인 허용. 토큰이 저장되지 않았으므로 해당 세션은 중복 로그인 감지 비활성.
            if (tokenResult is Result.Success) token else null
        } else {
            // 오프라인에서 새 토큰을 쓰면 큐에 남았다가 온라인 복귀 후 다른 기기를 로그아웃시킨다.
            // 기존 토큰을 유지하면 쓰기 없이도, 그사이 다른 기기가 로그인했는지 복귀 후 감지할 수 있다.
            user.activeSessionToken
        }

        userRepository.startUserSession(user.copy(activeSessionToken = activeToken))
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 세팅한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        CurrentUser.set(UserModel(uid, user.nickname, user.profileImg))

        return Result.Success(AutoLoginResult.LoggedIn(uid, user))
    }
}
