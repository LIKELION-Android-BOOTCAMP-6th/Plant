package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class StartStudyingSessionUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val repository: StudyingRepository
) {
    /**
     * 학습 화면 진입 시 로컬 백업 저장
     * "공부 중" 표시(studying/{uid})는 여기서 등록하지 않는다 — 타이머가 실제로 시작될 때 등록하고
     * 일시정지하면 지우므로(SaveStudyingUserUseCase, #525), 일시정지 상태로 진입해도 상위 3명에 나오지 않는다
     * [sessionId]는 학습 기록 문서 ID로 쓰이므로 세션을 시작할 때 만든 값을 그대로 넘긴다 (#453)
     */
    suspend operator fun invoke(tag: String, title: String, potId: String, time: Long, log: List<String>?, completed: List<Boolean>, startedAt: Long, isPaused: Boolean, sessionId: String) : Result<Unit> {
        return ensureCurrentUserUseCase {
            repository.saveLocalSession(
                StudyingSession(
                    userId = it.uid,
                    tag = tag,
                    title = title,
                    potId = potId,
                    time = time,
                    log = log,
                    startedAt = startedAt,
                    isPaused = isPaused,
                    status = StudyingSessionStatus.STUDYING,
                    sessionId = sessionId,
                    completed = completed
                )
            )
        } ?: Result.Failure(AppError.UnknownUser())
    }
}
