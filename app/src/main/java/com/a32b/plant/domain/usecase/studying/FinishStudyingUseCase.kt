package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class FinishStudyingUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 학습 기록 저장 — 기록 생성과 화분·유저 학습 시간 누적을 batch 하나로 보낸다 (#453)
     * - 같은 [sessionId]가 여러 번 와도 서버(보안 규칙)가 한 번만 반영한다
     * - 보상은 앱이 지급하지 않는다. 기록이 서버에 생기면 트리거가 지급한다
     * - 규칙이 거부할 것이 확실한 학습 시간(0 이하, 24시간 초과)은 보내지 않고 거부로 돌려준다
     *   (오프라인이면 연결된 뒤에야 거부되어, 그동안 기기 캐시에만 반영된 기록이 보이게 됨)
     * - 로컬 백업 삭제는 호출부가 결과에 따라 한다 ([StudySaveResult.Pending]이면 유지)
     */
    suspend operator fun invoke(
        potId: String,
        sessionId: String,
        timestamp: String,
        log: List<String>,
        completed: List<Boolean>,
        time: Long,
        startedAt: Long
    ): Result<StudySaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        // 세션 ID는 학습 시작(또는 복구) 시 백업에 먼저 저장된 값이어야 재전송해도 같은 기록으로 판단된다
        if (sessionId.isBlank()) return Result.Failure(AppError.Unknown())
        if (time <= 0L || time > StudyLog.MAX_STUDYING_TIME_MS) {
            return Result.Success(StudySaveResult.Rejected(StudySaveRejection.INVALID_TIME))
        }
        // 규칙이 목표와 완료 상태의 개수가 다르면 거부하므로 목표 개수에 맞춘다 (모자란 칸은 미완료)
        val normalizedCompleted = List(log.size) { index -> completed.getOrNull(index) ?: false }
        return repository.saveStudySession(
            uid = user.uid,
            potId = potId,
            sessionId = sessionId,
            log = StudyLog.write(timestamp, log, normalizedCompleted, time, startedAt)
        )
    }
}
