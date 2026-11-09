package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class GetRecoverySessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 복구 창에서 처리해야 할 로컬 백업 세션 조회 — 없으면 null
     * 현재 유저의 백업이고 쌓인 시간이 있을 때만 대상
     *
     * 종료(FINISHED) 백업은 저장 요청이 Firestore 대기열에 남아 있을 수 있어 바로 복구 창을 띄우지 않는다 (#453, #452)
     * 1. 대기열이 비기를 잠시 기다린다 → 비지 않으면(오프라인) 판단을 미루고 null (연결되면 대기열이 자동 전송)
     * 2. 대기열이 비었으면 서버에 같은 세션의 기록이 있는지 조회 → 있으면 이미 저장된 것이므로 백업만 지우고 null
     * 3. 서버에 없으면(거부됐거나 보내지 못함) 복구 창 대상
     * 공부 중(STUDYING) 백업은 앱이 비정상 종료된 세션이라 저장 요청을 보낸 적이 없으므로 바로 대상
     */
    suspend operator fun invoke(): Result<StudyingSession?> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        val session = when (val result = repository.readLocalSession()) {
            is Result.Success -> result.data?.takeIf { it.isRecoverableBy(user.uid) }
            is Result.Failure -> return result
        } ?: return Result.Success(null)

        // 세션 ID가 없는 이전 버전 백업은 서버 기록과 맞춰 볼 수 없으므로 그대로 복구 대상
        if (session.status != StudyingSessionStatus.FINISHED || session.sessionId.isBlank()) {
            return Result.Success(session)
        }
        if (!repository.awaitPendingWrites()) return Result.Success(null)

        return when (val saved = repository.isStudySessionSaved(user.uid, session.potId, session.sessionId)) {
            is Result.Success -> if (saved.data) {
                // 지우지 못하면 다음 홈 복귀에 같은 확인을 다시 하므로 실패는 따로 처리하지 않음
                repository.clearLocalSession()
                Result.Success(null)
            } else {
                Result.Success(session)
            }
            // 서버 조회 실패 — 복구해도 같은 세션 ID라 중복 저장되지 않으므로 복구 창을 띄운다
            is Result.Failure -> Result.Success(session)
        }
    }
}
