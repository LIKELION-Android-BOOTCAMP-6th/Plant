package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.util.NicknameRule
import javax.inject.Inject

class SetNicknameUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    /**
     * 첫 로그인 닉네임을 설정하고, 성공 시 실제로 저장된 닉네임을 돌려준다.
     *
     * 닉네임은 [NicknameRule.normalize]로 정규화한 값(앞뒤 공백 제거 + 분리된 한글 자모 합치기)으로 검사·저장한다.
     * 형식이 맞지 않으면 저장 요청 없이 [AppError.NicknameInvalid]를 반환한다.
     */
    suspend operator fun invoke(uid: String, nickname: String): Result<String> {
        val normalizedNickname = NicknameRule.normalize(nickname)
        val nicknameError = NicknameRule.validate(normalizedNickname)
        if (nicknameError != null) {
            return Result.Failure(AppError.NicknameInvalid(nicknameError))
        }

        // 중복 검사 + 닉네임 등록 + 첫 로그인 완료를 하나의 트랜잭션으로 처리한다.
        // (등록만 성공하고 완료가 실패해 닉네임이 선점된 채 남는 상태, 오프라인 write 대기를 방지)
        val completeResult = userRepository.completeFirstLogin(uid, normalizedNickname)
        if (completeResult is Result.Failure) return completeResult

        // users 문서가 갱신되면 UserRepository의 실시간 구독이 currentUser를 자동으로 최신화한다.
        // TODO: CurrentUser 전역 싱글톤 제거 대상.
        //  아직 CurrentUser를 직접 읽는 화면이 남아 있어 과도기 동안 함께 세팅한다.
        //  모든 화면이 UserRepository로 전환되면 이 줄과 di/CurrentUser.kt를 함께 삭제할 것.
        val profileImg = userRepository.currentUser.value?.profileImg.orEmpty()
        CurrentUser.set(UserModel(uid, normalizedNickname, profileImg))

        return Result.Success(normalizedNickname)
    }
}
