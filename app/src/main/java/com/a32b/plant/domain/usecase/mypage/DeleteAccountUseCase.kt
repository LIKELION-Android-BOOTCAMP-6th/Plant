package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.origin.OldNicknameRepository
import com.a32b.plant.origin.OldUserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val userRepository: OldUserRepository,
    private val nicknameRepository: OldNicknameRepository,
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        val uid = CurrentUser.uid
        val nickname = CurrentUser.nickname
        val firebaseUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("로그인 정보가 없습니다.")

        userRepository.deleteUser(uid)

        if (nickname.isNotBlank()) {
            nicknameRepository.deleteNickname(nickname)
        }

        firebaseUser.delete().await()
        CurrentUser.clear()
    }
}
