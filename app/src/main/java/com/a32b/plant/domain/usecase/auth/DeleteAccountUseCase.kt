package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(): Result<Unit> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return result // UnknownUser — 세션 만료 이벤트 이미 발송됨
        }
        val uid = user.uid
        val nickname = user.nickname

        // 1. Firestore 데이터 먼저 삭제 (인증 세션이 살아있는 동안)
        val deleteDataResult = userRepository.deleteUserData(uid)
        if (deleteDataResult is Result.Failure) return deleteDataResult

        if (nickname.isNotBlank()) {
            val deleteNicknameResult = userRepository.deleteNickname(nickname)
            if (deleteNicknameResult is Result.Failure) return deleteNicknameResult
        }

        // currentUser 실시간 구독을 먼저 끊어야 한다. Auth 계정을 지운 뒤에도 리스너가
        // 살아있으면 서버로부터 PERMISSION_DENIED를 받아 앱이 크래시한다.
        userRepository.endUserSession()
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 초기화한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        CurrentUser.clear()

        // 2. Firebase Auth 계정은 맨 마지막에 삭제
        val deleteAuthResult = authRepository.deleteAuthAccount()
        if (deleteAuthResult is Result.Failure) return deleteAuthResult

        return Result.Success(Unit)
    }
}
