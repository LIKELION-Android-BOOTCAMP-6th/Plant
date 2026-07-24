package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    // TODO: AuthRepository와 UserRepository의 로그아웃 계약이 준비되면
    // FirebaseAuth, CurrentUser 의존성을 제거한다.
    private val firebaseAuth: FirebaseAuth
) {
    operator fun invoke() {
        runCatching { firebaseAuth.signOut() }
        CurrentUser.clear()
    }
}
