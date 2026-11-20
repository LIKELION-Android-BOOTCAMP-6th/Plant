package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/**
 * 공부 중인 세션 백업의 학습 시간만 갱신 — 타이머 서비스의 주기 저장 (#668)
 * 전체 저장(UpdateLocalStudyingSessionUseCase)과 같이 로그인한 유저가 있을 때만 쓴다
 * 백그라운드에서 세션이 만료되면(중복 로그인·유저 문서 삭제) 앱으로 돌아와 서비스가 멈출 때까지 타이머는 계속 돌므로,
 * 유저 확인 없이 쓰면 만료 뒤의 시간까지 백업에 쌓여 같은 계정으로 다시 로그인했을 때 복구 창에 그대로 뜬다
 */
class UpdateLocalStudyingTimeUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {

    suspend operator fun invoke(sessionId: String, time: Long): Result<Unit> =
        ensureCurrentUserUseCase { repository.updateLocalSessionTime(sessionId, time) }
            ?: Result.Failure(AppError.UnknownUser())
}
