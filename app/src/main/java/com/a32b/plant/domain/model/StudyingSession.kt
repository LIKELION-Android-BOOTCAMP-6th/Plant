package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.StudyingSessionStatus
import java.util.UUID

data class StudyingSession(
    val userId: String,
    val tag: String,
    val title: String,
    val potId: String,
    val time: Long,
    val log: List<String>?,
    val startedAt: Long,
    val isPaused: Boolean,
    val status: StudyingSessionStatus,
    // 학습 기록 문서 ID로 쓰는 세션 ID — 같은 세션이 여러 번 저장 요청돼도 서버에는 한 번만 반영된다 (#453)
    // 이전 버전 백업에는 값이 없어 빈 문자열 (복구 시 새로 만들어 백업에 먼저 저장)
    val sessionId: String,
    // 학습 목표별 완료 체크 상태 (log와 같은 순서). 이전 버전 백업에는 값이 없어 빈 목록
    val completed: List<Boolean>
) {
    /**
     * 홈 복구 창에서 처리해야 할 백업인지 — 해당 유저의 백업이고 쌓인 시간이 있을 때만 복구 대상 (0초는 남길 기록이 없음)
     * 홈 복구 창(GetRecoverySessionUseCase)과 학습 화면 진입 시 종료 세션 차단이 같은 기준을 쓰도록 모델에 둠
     */
    fun isRecoverableBy(uid: String): Boolean = userId == uid && time > 0L

    /**
     * 목표 개수에 맞춘 완료 상태 — 모자란 칸은 미완료로 채우고 남는 칸은 버린다
     * (보안 규칙이 목표와 완료 상태의 개수가 다르면 저장을 거부함)
     */
    fun completedForLog(): List<Boolean> = List(log.orEmpty().size) { index -> completed.getOrNull(index) ?: false }

    companion object {
        /** 새 세션 ID — 학습 시작 시(또는 세션 ID가 없는 이전 버전 백업을 복구할 때) 한 번 만들어 백업에 저장 */
        fun newSessionId(): String = UUID.randomUUID().toString()
    }
}
