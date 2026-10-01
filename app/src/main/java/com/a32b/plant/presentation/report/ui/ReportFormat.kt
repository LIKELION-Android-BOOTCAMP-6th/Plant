package com.a32b.plant.presentation.report.ui

import com.a32b.plant.domain.model.MonthlyReport
import com.a32b.plant.domain.model.ReportPotRank
import com.a32b.plant.domain.model.ReportTimelineSegment
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun reportDuration(millis: Long): String {
    if (millis == 0L) return "0초"
    if (millis in 1..999) return "1초 미만"
    val seconds = millis / 1_000
    return buildList {
        if (seconds / 3_600 > 0) add("${seconds / 3_600}시간")
        if (seconds / 60 % 60 > 0) add("${seconds / 60 % 60}분")
        if (seconds % 60 > 0) add("${seconds % 60}초")
    }.joinToString(" ")
}

internal fun reportAverageDuration(millis: Double): String = when {
    millis == 0.0 -> "0초"
    millis > 0.0 && millis < 1_000.0 -> "1초 미만"
    else -> reportDuration(millis.toLong())
}

// 예: "9월 1일~26일 합계를 26일로 나눔", 하루뿐이면 "9월 1일 합계를 1일로 나눔"
internal fun reportAverageCaption(report: MonthlyReport): String {
    val month = report.month.monthValue
    val period = if (report.averageDayCount == 1) "${month}월 1일"
        else "${month}월 1일~${report.averageBasisDate.dayOfMonth}일"
    return "$period 합계를 ${report.averageDayCount}일로 나눔"
}

internal fun reportPotRank(rank: ReportPotRank?): String =
    if (rank == null) "기록 없음"
    else buildString {
        append(rank.potName)
        if (rank.otherTiedCount > 0) append(" 외 ${rank.otherTiedCount}개 · 각 ")
        else append(" · ")
        append(reportDuration(rank.totalMillis))
    }

internal fun reportSegmentTime(segment: ReportTimelineSegment): String {
    val start = reportMinute(segment.startMinute)
    return if (segment.startMinute == segment.endMinute) "$start (1분 미만)"
    else "$start~${reportMinute(segment.endMinute)}"
}

internal fun reportMinute(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)

// 기록의 시각 조각 한 줄: 선택한 날짜 조각은 시각만, 다른 날짜 조각(자정 넘김)은 날짜를 밝힌다. "전날" 표현은 쓰지 않는다.
internal fun reportRecordSegmentLine(segment: ReportTimelineSegment, selectedDate: LocalDate): String =
    if (segment.date == selectedDate) reportSegmentTime(segment)
    else "${segment.date.format(reportMonthDayFormat)} ${reportSegmentTime(segment)}"

internal val reportMonthDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일")
