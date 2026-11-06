package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class RecoverStudySessionUseCase @Inject constructor(
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val repository: StudyingRepository
) {
    /**
     * 백업 세션을 학습 기록으로 저장한 뒤 로컬 백업 삭제
     * 제한 시간은 기록 저장에만 적용 — 저장이 끝난 뒤 로컬 삭제가 끊겨 백업이 남으면 다시 저장될 수 있음
     * "공부 중" 표시(studying/{uid}) 삭제는 복구 결과와 무관하므로 여기서 하지 않음 — 호출부가 기다리지 않고 따로 요청
     */
    suspend operator fun invoke(session: StudyingSession): Result<Unit> {
        val startedAtDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(session.startedAt),
            ZoneId.systemDefault()
        )
        val endedAtMillis = session.startedAt + session.time
        val timestamp = "${TimeFormatter.formatToKoreanDate(startedAtDateTime)} " +
                "${TimeFormatter.formatToTimeOnly(session.startedAt)} ~ " +
                TimeFormatter.formatToTimeOnly(endedAtMillis)

        val saveResult = withTimeoutOrNull(FinishStudyingUseCase.SAVE_TIMEOUT) {
            finishStudyingUseCase(
                potId = session.potId,
                timestamp = timestamp,
                log = session.log ?: emptyList(),
                completed = emptyList(), // 로컬 백업에는 완료 체크 상태가 저장되지 않음
                time = session.time,
                startedAt = session.startedAt
            )
        } ?: return Result.Failure(AppError.Network())
        if (saveResult is Result.Failure) return saveResult

        return repository.clearLocalSession()
    }
}
