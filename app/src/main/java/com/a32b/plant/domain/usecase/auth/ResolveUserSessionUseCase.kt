package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.model.SignInResult
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

/**
 * 이메일/구글 로그인 성공 이후 공통으로 수행하는 처리.
 * Firestore에 유저 문서가 없으면 새로 만들고, currentUser 상태를 세팅한 뒤
 * 첫 로그인 여부를 반환한다.
 */
class ResolveUserSessionUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(uid: String): Result<SignInResult> {
        val getUserResult = userRepository.getUser(uid)
        if (getUserResult is Result.Failure) return getUserResult
        val existingUser = (getUserResult as Result.Success).data

        val user = if (existingUser != null) {
            existingUser
        } else {
            val createResult = userRepository.createUser(uid)
            if (createResult is Result.Failure) return createResult
            (createResult as Result.Success).data
        }

        userRepository.startUserSession(user)
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 세팅한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        CurrentUser.set(UserModel(uid, user.nickname, user.profileImg))

        return Result.Success(
            SignInResult(
                uid = uid,
                nickname = user.nickname,
                isFirstLogin = user.isFirstLogin == true
            )
        )
    }
}
