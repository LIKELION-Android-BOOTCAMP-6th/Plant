package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.origin.OldNicknameRepository
import com.a32b.plant.origin.OldUserRepository
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: OldUserRepository,
    private val nicknameRepository: OldNicknameRepository
) {
    suspend operator fun invoke(
        currentNickname: String,
        currentImageLevel: String,
        newNickname: String,
        newImageLevel: String
    ): Result<Unit> = runCatching {
        val nicknameError = validateNickname(newNickname)
        if (nicknameError != null) {
            throw IllegalArgumentException(nicknameError)
        }

        if (newNickname == currentNickname && newImageLevel == currentImageLevel) {
            throw IllegalArgumentException("변경사항이 없습니다.")
        }

        if (newNickname != currentNickname) {
            if (nicknameRepository.isNicknameTaken(newNickname)) {
                throw IllegalArgumentException("이미 사용중인 닉네임입니다")
            }

            nicknameRepository.registerNickname(newNickname)
            nicknameRepository.deleteNickname(currentNickname)
        }

        userRepository.updateNicknameAndImage(
            CurrentUser.uid,
            newNickname,
            newImageLevel
        )

        CurrentUser.set(
            UserModel(
                uid = CurrentUser.uid,
                nickname = newNickname,
                profileImg = newImageLevel
            )
        )
    }

    private fun validateNickname(nickname: String): String? {
        val len = nickname.length
        return if (len !in 2..10) {
            "닉네임은 2자 이상 10자 이하로 입력해주세요."
        } else {
            null
        }
    }
}