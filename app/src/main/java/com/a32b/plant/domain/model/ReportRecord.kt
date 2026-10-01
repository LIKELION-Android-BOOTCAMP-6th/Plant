package com.a32b.plant.domain.model

import java.time.LocalDateTime

data class ReportRecord(
    val potId: String,
    val potName: String,
    val logId: String,
    val title: String,
    val studyingTime: Long,
    val savedAt: LocalDateTime,
    val timelineStatus: ReportTimelineStatus,
    // 선택 월 밖 날짜도 포함한 이 기록의 시각 조각 전체. 제목 해석 실패·24시간 이상이면 빈 리스트.
    val allSegments: List<ReportTimelineSegment>
)

enum class ReportTimelineStatus {
    // 제목 해석과 길이 조건을 통과한 상태. 선택 월에 표시할 구간이 있다는 뜻은 아니다.
    SHOWN,
    TITLE_UNPARSABLE,
    TOO_LONG
}
