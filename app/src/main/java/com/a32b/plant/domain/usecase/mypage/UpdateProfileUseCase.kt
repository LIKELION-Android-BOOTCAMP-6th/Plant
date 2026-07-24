package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.Result
import com.a32b.plant.origin.OldNicknameRepository
import com.a32b.plant.origin.OldUserRepository
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    // TODO: UserRepository의 프로필 갱신 API와 닉네임 갱신 책임이 확정되면
    // OldUserRepository, OldNicknameRepository, CurrentUser 의존성을 제거한다.
    private val userRepository: OldUserRepository,
    private val nicknameRepository: OldNicknameRepository
) {
    suspend operator fun invoke(
        currentNickname: String,
        currentImageLevel: String,
        newNickname: String,
        newImageLevel: String
    ): Result<Unit> {
        val nicknameError = validateNickname(newNickname)
        if (nicknameError != null) {
            return Result.Failure(AppError.Custom(nicknameError))
        }

        if (newNickname == currentNickname && newImageLevel == currentImageLevel) {
            return Result.Failure(AppError.Custom("변경사항이 없습니다."))
        }

        return try {
            if (newNickname != currentNickname) {
                if (nicknameRepository.isNicknameTaken(newNickname)) {
                    return Result.Failure(AppError.Custom("이미 사용중인 닉네임입니다"))
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
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseNetworkException) {
            Result.Failure(AppError.Network())
        } catch (_: Exception) {
            Result.Failure(AppError.Custom("프로필 수정에 실패했습니다."))
        }
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
