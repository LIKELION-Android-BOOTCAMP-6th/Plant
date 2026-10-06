package com.a32b.plant.domain.model

import java.time.LocalDateTime

data class ReportRecord(
    val potName: String,
    val tagName: String,
    val studyingTime: Long,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime
)
