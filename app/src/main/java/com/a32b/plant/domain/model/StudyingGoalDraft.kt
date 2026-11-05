package com.a32b.plant.domain.model

/** 학습 목표 입력 창에서 작성 중인 글 — 강제 종료 후 같은 계정·같은 화분으로 다시 들어왔을 때 복원 */
data class StudyingGoalDraft(
    val userId: String,
    val potId: String,
    val logs: List<String>
)
