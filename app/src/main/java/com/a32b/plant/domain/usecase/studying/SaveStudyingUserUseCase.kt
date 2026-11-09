package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class SaveStudyingUserUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val repository: StudyingRepository
) {
    /**
     * "공부 중" 표시(studying/{uid})를 현재 유저 정보로 등록·갱신
     * 문서 전체를 다시 쓰므로 예약 함수가 지운 뒤에도 닉네임·프로필과 함께 다시 만들어진다 (#525)
     * 유저 정보가 없으면 세션 만료 알림은 ensureCurrentUserUseCase가 보낸다
     */
    suspend operator fun invoke(tag: String, time: Long): Result<Unit> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.saveStudyingUser(
            StudyingUser(user.uid, user.nickname, user.profileImg, tag, time)
        )
    }
}
