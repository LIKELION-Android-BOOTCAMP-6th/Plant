package com.a32b.plant.domain.model

import java.time.LocalDate
import java.time.YearMonth

sealed class PreparedReportMonth {
    data class Valid(
        val month: YearMonth,
        val records: List<ReportRecord>,
        val segments: List<ReportTimelineSegment>,
        val calendarDates: Set<LocalDate>
    ) : PreparedReportMonth()

    object CalculationError : PreparedReportMonth()
}
