package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.NicknameRule
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 프로필(닉네임·이미지)을 변경하고, 성공 시 실제로 저장된 닉네임을 돌려준다.
     *
     * 닉네임은 [NicknameRule.normalize]로 정규화한 값(앞뒤 공백 제거 + 분리된 한글 자모 합치기)으로 검사·저장한다.
     * 정규화한 값이 기존 닉네임(정규화한 값)과 같으면 닉네임은 바뀌지 않은 것으로 보고 기존 값을 그대로 둔다.
     * (공백이나 분리형 자모가 들어간 기존 닉네임 사용자가 이미지만 바꿀 때 닉네임 변경으로 처리되지 않게)
     */
    suspend operator fun invoke(
        newNickname: String,
        newImageLevel: String
    ): Result<String> {
        val currentUser = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return result
        }

        val normalizedNickname = NicknameRule.normalize(newNickname)
        val isNicknameChanged = normalizedNickname != NicknameRule.normalize(currentUser.nickname)

        if (!isNicknameChanged && newImageLevel == currentUser.profileImg) {
            return Result.Failure(AppError.Custom("변경사항이 없습니다."))
        }

        if (isNicknameChanged) {
            val nicknameError = validateNickname(normalizedNickname)
            if (nicknameError != null) {
                return Result.Failure(AppError.Custom(nicknameError))
            }
        }

        val savedNickname = if (isNicknameChanged) normalizedNickname else currentUser.nickname

        if (isNicknameChanged) {
            when (val result = userRepository.isNicknameTaken(savedNickname)) {
                is Result.Success -> {
                    if (result.data) {
                        return Result.Failure(AppError.NicknameTaken())
                    }
                }

                is Result.Failure -> return result
            }

            val registerResult = userRepository.registerNickname(currentUser.uid, savedNickname)
            if (registerResult is Result.Failure) {
                return registerResult
            }

            val deleteResult = userRepository.deleteNickname(currentUser.nickname)
            if (deleteResult is Result.Failure) {
                return rollbackRegisteredNickname(
                    newNickname = savedNickname,
                    originalFailure = deleteResult
                )
            }
        }
        val updatedUser = currentUser.copy(
            nickname = savedNickname,
            profileImg = newImageLevel
        )
        val updateResult = userRepository.updateProfile(updatedUser)

        if (updateResult is Result.Failure && isNicknameChanged) {
            return rollbackNicknameChange(
                uid = currentUser.uid,
                previousNickname = currentUser.nickname,
                newNickname = savedNickname,
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

        return updateResult.map { savedNickname }
    }

    private suspend fun rollbackNicknameChange(
        uid: String,
        previousNickname: String,
        newNickname: String,
        originalFailure: Result.Failure
    ): Result.Failure {
        val restoreResult = userRepository.registerNickname(uid, previousNickname)
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
        return when {
            !NicknameRule.isValidLength(nickname) -> NicknameRule.LENGTH_MESSAGE
            !NicknameRule.isUsable(nickname) -> NicknameRule.UNUSABLE_MESSAGE
            else -> null
        }
    }
}
