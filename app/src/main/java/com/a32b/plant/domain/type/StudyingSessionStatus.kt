package com.a32b.plant.domain.type

/** 로컬 백업 세션의 진행 상태 */
enum class StudyingSessionStatus {
    /** 공부 중 (일시정지 포함) — 학습 화면 재진입 시 이어서 복원 */
    STUDYING,

    /** 종료했지만 기록 저장이 끝나지 않음 — 복원하지 않고 홈 복구 창에서 처리 */
    FINISHED
}
