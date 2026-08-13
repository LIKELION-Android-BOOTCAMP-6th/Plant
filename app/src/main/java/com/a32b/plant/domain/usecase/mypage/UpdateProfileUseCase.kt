package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(
        newNickname: String,
        newImageLevel: String
    ): Result<Unit> {
        val currentUser = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }

        val nicknameError = validateNickname(newNickname)
        if (nicknameError != null) {
            return Result.Failure(AppError.Custom(nicknameError))
        }

        if (newNickname == currentUser.nickname &&
            newImageLevel == currentUser.profileImg
        ) {
            return Result.Failure(AppError.Custom("변경사항이 없습니다."))
        }

        if (newNickname != currentUser.nickname) {
            when (val result = userRepository.isNicknameTaken(newNickname)) {
                is Result.Success -> {
                    if (result.data) {
                        return Result.Failure(
                            AppError.Custom("이미 사용중인 닉네임입니다.")
                        )
                    }
                }

                is Result.Failure -> return result
            }

            val registerResult = userRepository.registerNickname(newNickname)
            if (registerResult is Result.Failure) {
                return registerResult
            }

            val deleteResult = userRepository.deleteNickname(currentUser.nickname)
            if (deleteResult is Result.Failure) {
                return rollbackRegisteredNickname(
                    newNickname = newNickname,
                    originalFailure = deleteResult
                )
            }
        }
        val updatedUser = currentUser.copy(
            nickname = newNickname,
            profileImg = newImageLevel
        )
        val updateResult = userRepository.updateProfile(updatedUser)

        if (updateResult is Result.Failure &&
            newNickname != currentUser.nickname
        ) {
            return rollbackNicknameChange(
                previousNickname = currentUser.nickname,
                newNickname = newNickname,
                originalFailure = updateResult
            )
        }

        if (updateResult is Result.Success) {
            // TODO: 아직 CurrentUser를 읽는 화면이 남아 있어 과도기 동안 함께 갱신한다.
            //  모든 화면이 UserRepository로 전환되면 이 동기화를 제거한다.
            CurrentUser.set(
                UserModel(
                    uid = updatedUser.uid,
                    nickname = updatedUser.nickname,
                    profileImg = updatedUser.profileImg
                )
            )
        }

        return updateResult
    }

    private suspend fun rollbackNicknameChange(
        previousNickname: String,
        newNickname: String,
        originalFailure: Result.Failure
    ): Result.Failure {
        val restoreResult = userRepository.registerNickname(previousNickname)
        if (restoreResult is Result.Failure) {
            originalFailure.error.addSuppressed(restoreResult.error)
        }
        val cleanupResult = userRepository.deleteNickname(newNickname)
        if (cleanupResult is Result.Failure) {
            originalFailure.error.addSuppressed(cleanupResult.error)
        }

        return originalFailure
    }

    private suspend fun rollbackRegisteredNickname(
        newNickname: String,
        originalFailure: Result.Failure
    ): Result.Failure {
        val rollbackResult = userRepository.deleteNickname(newNickname)

        if (rollbackResult is Result.Failure) {
            originalFailure.error.addSuppressed(rollbackResult.error)
        }

        return originalFailure
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
