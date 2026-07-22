package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.UserRepository
import javax.inject.Inject

/** 성공/실패와 무관하게 로그아웃되므로 Result를 사용하지 않는다. */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke() {
        // currentUser 실시간 구독을 먼저 끊어야 한다. signOut()이 먼저 실행되면
        // 아직 살아있는 리스너가 서버로부터 PERMISSION_DENIED를 받아 앱이 크래시한다.
        userRepository.endUserSession()
        authRepository.signOut()
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 초기화한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        CurrentUser.clear()
    }
}
