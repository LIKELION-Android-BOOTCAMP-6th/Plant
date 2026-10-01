package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.DailyReport
import com.a32b.plant.domain.model.ReportRecord
import com.a32b.plant.domain.model.ReportTimelineSegment
import com.a32b.plant.domain.model.ReportTimelineStatus
import java.time.LocalDate

@Composable
internal fun DailyReportContent(report: DailyReport) {
    val date = report.date
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReportPeriodTitle(date)
        // 저장 기록이 없으면 "0초 + 기록 없음" 한 번만 보여주고, 최고·최저 줄과 목록 카드는 숨긴다(반복 제거).
        val hasRecords = report.savedRecords.isNotEmpty()
        ReportCard(spacing = 12.dp) {
            ReportField(
                "총 공부 시간", reportDuration(report.totalMillis), emphasized = true,
                caption = if (hasRecords) "이 날짜에 저장된 기록 기준" else "이 날짜에 저장된 기록 없음"
            )
            if (hasRecords) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ReportField("가장 많이 공부한 화분", reportPotRank(report.potComparison.highest))
                ReportField(
                    "가장 적게 공부한 화분",
                    if (report.potComparison.lowest == null) "비교할 다른 화분 없음"
                    else reportPotRank(report.potComparison.lowest)
                )
            }
        }
        if (report.otherDaySegments.isNotEmpty()) {
            ReportCard {
                Text("다른 날 저장된 구간", style = MaterialTheme.typography.titleMedium)
                report.otherDaySegments.forEachIndexed { index, segment ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    TimelineDetail(segment)
                }
            }
        }
        if (hasRecords) {
            ReportCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("이 날짜에 저장된 기록", style = MaterialTheme.typography.titleMedium)
                    Text("시각은 학습 화면을 연 때부터 저장 때까지예요", style = MaterialTheme.typography.labelMedium)
                }
                report.savedRecords.forEachIndexed { index, record ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    key(date, record.potId, record.logId) {
                        SavedReportRecord(record, date)
                    }
                }
            }
        }
    }
}

// 정상 기록은 시각 줄만 보여준다. 원본 제목·저장 시각은 시각을 못 보여줄 때만 대신 보여준다(반복 정보 제거).
@Composable
internal fun SavedReportRecord(record: ReportRecord, date: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${record.potName} · ${reportDuration(record.studyingTime)}", style = MaterialTheme.typography.titleMedium)
        when (record.timelineStatus) {
            ReportTimelineStatus.SHOWN -> record.allSegments.forEach { segment ->
                Text(reportRecordSegmentLine(segment, date), style = MaterialTheme.typography.bodyMedium)
            }
            ReportTimelineStatus.TITLE_UNPARSABLE -> {
                Text("기록 시각을 확인할 수 없어요.", style = MaterialTheme.typography.bodyMedium)
                if (record.title.isNotBlank()) {
                    Text("원본 제목 · ${record.title}", style = MaterialTheme.typography.labelMedium)
                }
            }
            ReportTimelineStatus.TOO_LONG -> {
                Text("공부 시간이 24시간 이상이라 시각을 표시할 수 없어요.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "저장 · ${TimeFormatter.formatToKoreanDate(record.savedAt)} ${TimeFormatter.formatToTimeOnly(record.savedAt)}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun TimelineDetail(segment: ReportTimelineSegment) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(segment.potName, style = MaterialTheme.typography.titleMedium)
        Text(reportSegmentTime(segment), style = MaterialTheme.typography.bodyMedium)
        Text("${TimeFormatter.formatToKoreanDate(segment.savedDate.atStartOfDay())} 저장", style = MaterialTheme.typography.labelMedium)
    }
}
