package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.StudyingSessionStatus

data class StudyingSession(
    val userId: String,
    val tag: String,
    val title: String,
    val potId: String,
    val time: Long,
    val log: List<String>?,
    val startedAt: Long,
    val isPaused: Boolean,
    val status: StudyingSessionStatus
) {
    /**
     * 홈 복구 창에서 처리해야 할 백업인지 — 해당 유저의 백업이고 쌓인 시간이 있을 때만 복구 대상 (0초는 남길 기록이 없음)
     * 홈 복구 창(GetRecoverySessionUseCase)과 학습 화면 진입 시 종료 세션 차단이 같은 기준을 쓰도록 모델에 둠
     */
    fun isRecoverableBy(uid: String): Boolean = userId == uid && time > 0L
}
