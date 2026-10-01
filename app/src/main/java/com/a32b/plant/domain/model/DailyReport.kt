package com.a32b.plant.domain.model

import java.time.LocalDate

data class DailyReport(
    val date: LocalDate,
    val totalMillis: Long,
    val potComparison: ReportPotComparison,
    val savedRecords: List<ReportRecord>,
    // 선택한 날에 걸치지만 다른 날 저장된 기록의 조각(그래프 없는 목록형 화면의 별도 영역용)
    val otherDaySegments: List<ReportTimelineSegment>
)
