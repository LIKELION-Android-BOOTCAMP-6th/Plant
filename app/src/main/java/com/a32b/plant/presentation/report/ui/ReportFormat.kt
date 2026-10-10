package com.a32b.plant.presentation.report.ui

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.model.ReportDailyTotal
import com.a32b.plant.domain.model.ReportRecord

// 예: "공부한 26일 합계를 26일로 나눔". 공부한 날이 없으면 평균이 0이라 설명을 붙이지 않는다.
fun reportAverageCaption(report: MonthlyReport): String? {
    val dayCount = report.recordDates.size
    return if (dayCount == 0) null else "공부한 ${dayCount}일 합계를 ${dayCount}일로 나눔"
}

// 예: "[자격증] 정처기 · 1시간 20분"
fun reportRecordTitle(record: ReportRecord): String =
    "[${record.tagName}] ${record.potName} · ${TimeFormatter.formatToKoreanDuration(record.studyingTime)}"

// 예: "2026년 10월 05일 · 1시간 20분"
fun reportDayTotal(day: ReportDailyTotal): String =
    "${TimeFormatter.formatToKoreanDate(day.date.atStartOfDay())} · ${TimeFormatter.formatToKoreanDuration(day.totalMillis)}"
