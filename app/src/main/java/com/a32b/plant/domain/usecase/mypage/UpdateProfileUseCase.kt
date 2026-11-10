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
     * 닉네임 중복 확인과 이전 닉네임 문서 삭제는 Repository 트랜잭션에서 처리하므로 여기서는 하지 않는다.
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
            val nicknameError = NicknameRule.validate(normalizedNickname)
            if (nicknameError != null) {
                return Result.Failure(AppError.NicknameInvalid(nicknameError))
            }
        }

        val savedNickname = if (isNicknameChanged) normalizedNickname else currentUser.nickname

        val updatedUser = currentUser.copy(
            nickname = savedNickname,
            profileImg = newImageLevel
        )
        val updateResult = userRepository.updateProfile(updatedUser)

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
}
