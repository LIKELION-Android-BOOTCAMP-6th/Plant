package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    operator fun invoke(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
        CurrentUser.clear()
    }
}
