package com.a32b.plant.domain.model

import java.time.LocalDate

data class DailyReport(
    val date: LocalDate,
    val totalMillis: Long,
    val highest: ReportRecord?,
    // 기록이 1개면 최고 기록만 보여주므로 null
    val lowest: ReportRecord?,
    val records: List<ReportRecord>
)
