package com.a32b.plant.domain.model

import java.time.LocalDate

data class ReportTimelineSegment(
    val potId: String,
    val logId: String,
    val potName: String,
    val date: LocalDate,
    val startMinute: Int,
    val endMinute: Int,
    val savedDate: LocalDate
)
