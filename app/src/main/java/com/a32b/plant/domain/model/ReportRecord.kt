package com.a32b.plant.domain.model

import java.time.LocalDateTime

data class ReportRecord(
    val potName: String,
    val tagName: String,
    val studyingTime: Long,
    val startedAt: LocalDateTime,
    // 저장 시각(StudyLog.createAt)을 리포트의 종료 시각으로 표시한다.
    val endedAt: LocalDateTime
)
