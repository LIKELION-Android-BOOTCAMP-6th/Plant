package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.PreparedReportMonth
import com.a32b.plant.domain.model.ReportMonthData
import com.a32b.plant.domain.model.ReportRecord
import com.a32b.plant.domain.model.ReportTimelineSegment
import com.a32b.plant.domain.model.ReportTimelineStatus
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class PrepareReportMonthUseCase @Inject constructor() {
    operator fun invoke(month: YearMonth, monthData: ReportMonthData): PreparedReportMonth {
        val records = mutableListOf<ReportRecord>()
        val segments = mutableListOf<ReportTimelineSegment>()
        val calendarDates = mutableSetOf<LocalDate>()
        var monthTotal = 0L

        for (potLogs in monthData.potLogs) {
            for (log in potLogs.logs) {
                // 기간 조회에는 createAt이 있는 문서만 들어오지만, nullable 도메인 계약도 지킨다.
                val savedMillis = log.createAt ?: continue
                val savedAt = Instant.ofEpochMilli(savedMillis)
                    .atZone(REPORT_ZONE)
                    .toLocalDateTime()
                val savedDate = savedAt.toLocalDate()
                val savedInMonth = YearMonth.from(savedDate) == month

                if (savedInMonth) {
                    if (log.studyingTime < 0L || Long.MAX_VALUE - monthTotal < log.studyingTime) {
                        return PreparedReportMonth.CalculationError
                    }
                    monthTotal += log.studyingTime
                }

                val titleRange = parseTitleRange(log.title)
                val timelineStatus = when {
                    titleRange == null -> ReportTimelineStatus.TITLE_UNPARSABLE
                    log.studyingTime >= DAY_MILLIS -> ReportTimelineStatus.TOO_LONG
                    else -> ReportTimelineStatus.SHOWN
                }

                val allSegments = if (titleRange != null && log.studyingTime < DAY_MILLIS) {
                    segmentsFor(
                        range = titleRange,
                        potId = potLogs.pot.id,
                        logId = log.id,
                        potName = potLogs.pot.name,
                        savedDate = savedDate
                    )
                } else {
                    emptyList()
                }

                if (savedInMonth) {
                    records += ReportRecord(
                        potId = potLogs.pot.id,
                        potName = potLogs.pot.name,
                        logId = log.id,
                        title = log.title,
                        studyingTime = log.studyingTime,
                        savedAt = savedAt,
                        timelineStatus = timelineStatus,
                        allSegments = allSegments
                    )
                    calendarDates += savedDate
                }

                val matchingSegments = allSegments.filter { YearMonth.from(it.date) == month }
                segments += matchingSegments
                calendarDates += matchingSegments.map { it.date }
            }
        }

        return PreparedReportMonth.Valid(
            month = month,
            records = records,
            segments = segments,
            calendarDates = calendarDates
        )
    }
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private val REPORT_ZONE = ZoneId.of("Asia/Seoul")
private val TITLE_PATTERN =
    Regex("""([0-9]{4})년 ([0-9]{2})월 ([0-9]{2})일 ([0-9]{2}):([0-9]{2}) ~ ([0-9]{2}):([0-9]{2})""")

private data class TitleRange(val start: LocalDateTime, val end: LocalDateTime)

private fun parseTitleRange(title: String): TitleRange? {
    val match = TITLE_PATTERN.matchEntire(title) ?: return null
    val (year, month, day, startHour, startMinute, endHour, endMinute) = match.destructured
    if (year.toInt() == 0) return null

    return try {
        val endDate = LocalDate.of(year.toInt(), month.toInt(), day.toInt())
        val startTime = LocalTime.of(startHour.toInt(), startMinute.toInt())
        val endTime = LocalTime.of(endHour.toInt(), endMinute.toInt())
        val startDate = if (startTime.isAfter(endTime)) endDate.minusDays(1) else endDate
        TitleRange(startDate.atTime(startTime), endDate.atTime(endTime))
    } catch (_: DateTimeException) {
        null
    }
}

private fun segmentsFor(
    range: TitleRange,
    potId: String,
    logId: String,
    potName: String,
    savedDate: LocalDate
): List<ReportTimelineSegment> {
    val result = mutableListOf<ReportTimelineSegment>()

    fun add(date: LocalDate, startMinute: Int, endMinute: Int) {
        result += ReportTimelineSegment(
            potId = potId,
            logId = logId,
            potName = potName,
            date = date,
            startMinute = startMinute,
            endMinute = endMinute,
            savedDate = savedDate
        )
    }

    val startDate = range.start.toLocalDate()
    val endDate = range.end.toLocalDate()
    val startMinute = range.start.hour * 60 + range.start.minute
    val endMinute = range.end.hour * 60 + range.end.minute

    if (startDate == endDate) {
        // 동일 HH:mm은 0분 길이의 점이며 공부 시간을 임의로 늘리지 않는다.
        add(startDate, startMinute, endMinute)
    } else {
        add(startDate, startMinute, 1440)
        // [시작, 종료)에서 정확히 00:00 종료는 다음 날 조각을 만들지 않는다.
        if (endMinute > 0) add(endDate, 0, endMinute)
    }

    return result
}
