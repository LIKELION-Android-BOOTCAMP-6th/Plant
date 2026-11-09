package com.a32b.plant.domain.type

/**
 * 보안 규칙이 학습 기록 저장을 거부한 사유 (#453)
 * 규칙은 사유를 알려주지 않으므로, 거부된 뒤 서버 상태(화분·유저·기록)를 조회해 앱이 판단한다
 */
enum class StudySaveRejection(val message: String) {
    /** 이전 세션의 종료 시각(users.lastStudyEndedAt)보다 먼저 시작한 세션 */
    OVERLAPPED("이전 학습 기록과 시간이 겹쳐 저장할 수 없습니다."),

    /** 화분이 없음 (삭제됨) */
    POT_NOT_FOUND("화분이 삭제되어 학습 기록을 저장할 수 없습니다."),

    /** 이미 완료된 화분 */
    POT_COMPLETED("이미 완료된 화분이라 학습 기록을 저장할 수 없습니다."),

    /** 학습 시간이 0 이하이거나 24시간 초과 */
    INVALID_TIME("학습 시간이 올바르지 않아 저장할 수 없습니다."),

    /** 그 외 (기기 시계가 크게 어긋남 등) */
    UNKNOWN("학습 기록을 저장할 수 없습니다.")
}
