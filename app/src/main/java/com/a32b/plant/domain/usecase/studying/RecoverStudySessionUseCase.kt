package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudyingSessionStatus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class RecoverStudySessionUseCase @Inject constructor(
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val repository: StudyingRepository
) {
    /**
     * 백업 세션을 학습 기록으로 저장 (#453)
     * - 세션 ID가 없는 이전 버전 백업은 새 ID를 만들고, 완료 상태를 목표 개수에 맞춘 뒤 **백업에 먼저 저장**한다
     *   → 저장이 대기 중(Pending)으로 남아 다시 복구해도 같은 세션 ID로 보내 중복 저장되지 않음
     * - 종료 상태(FINISHED)로 백업해 두어, 저장 요청이 대기열에 남은 동안 홈 복구 창이 다시 뜨지 않게 한다
     * - 저장됨 / 이미 저장됨 / 다시 보내도 안 되는 거부 → 로컬 백업 삭제, 대기 중(Pending) → 백업 유지
     * "공부 중" 표시(studying/{uid}) 삭제는 복구 결과와 무관하므로 여기서 하지 않음 — 호출부가 기다리지 않고 따로 요청
     */
    suspend operator fun invoke(session: StudyingSession): Result<StudySaveResult> {
        val prepared = session.copy(
            sessionId = session.sessionId.ifBlank { StudyingSession.newSessionId() },
            completed = session.completedForLog(),
            status = StudyingSessionStatus.FINISHED
        )
        if (prepared != session) {
            val backupResult = repository.saveLocalSession(prepared)
            if (backupResult is Result.Failure) return backupResult
        }

        val startedAtDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(prepared.startedAt),
            ZoneId.systemDefault()
        )
        val endedAtMillis = prepared.startedAt + prepared.time
        val timestamp = "${TimeFormatter.formatToKoreanDate(startedAtDateTime)} " +
                "${TimeFormatter.formatToTimeOnly(prepared.startedAt)} ~ " +
                TimeFormatter.formatToTimeOnly(endedAtMillis)

        val saveResult = finishStudyingUseCase(
            potId = prepared.potId,
            sessionId = prepared.sessionId,
            timestamp = timestamp,
            log = prepared.log ?: emptyList(),
            completed = prepared.completed,
            time = prepared.time,
            startedAt = prepared.startedAt
        )
        val result = when (saveResult) {
            is Result.Success -> saveResult.data
            is Result.Failure -> return saveResult
        }
        if (result == StudySaveResult.Pending) return Result.Success(result)

        // 저장 확인 뒤 백업 삭제가 실패해도 다음 복구는 같은 세션 ID로 보내 "이미 저장됨"으로 처리된다
        val clearResult = repository.clearLocalSession()
        if (clearResult is Result.Failure) return clearResult
        return Result.Success(result)
    }
}
