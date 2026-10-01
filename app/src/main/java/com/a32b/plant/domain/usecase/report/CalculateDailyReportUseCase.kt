package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.model.PreparedReportMonth
import com.a32b.plant.domain.model.ReportRecord
import java.time.LocalDate
import javax.inject.Inject

class CalculateDailyReportUseCase @Inject constructor(
    private val calculatePotComparison: CalculatePotComparisonUseCase
) {
    operator fun invoke(prepared: PreparedReportMonth.Valid, date: LocalDate): DailyReport {
        val records = prepared.records.filter { it.savedAt.toLocalDate() == date }
            .sortedWith(
                compareByDescending<ReportRecord> { it.savedAt }
                    .thenBy { it.potId }
                    .thenBy { it.logId }
            )
        val otherDaySegments = prepared.segments.asSequence()
            .filter { it.date == date && it.savedDate != date }
            .sortedWith(compareBy({ it.startMinute }, { it.endMinute }, { it.potId }, { it.logId }))
            .toList()

        return DailyReport(
            date = date,
            totalMillis = records.sumOf { it.studyingTime },
            potComparison = calculatePotComparison(records),
            savedRecords = records,
            otherDaySegments = otherDaySegments
        )
    }
}
