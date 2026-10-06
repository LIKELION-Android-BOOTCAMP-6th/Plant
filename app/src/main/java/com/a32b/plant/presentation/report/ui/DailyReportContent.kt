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

@Composable
fun DailyReportContent(report: DailyReport) {
    val date = report.date
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ReportPeriodTitle(date)
        // 시작한 기록이 없으면 "0초 + 기록 없음" 한 번만 보여주고, 최고·최저 줄과 목록 카드는 숨긴다(반복 제거).
        val hasRecords = report.records.isNotEmpty()
        ReportCard(spacing = 12.dp) {
            ReportField(
                "총 공부 시간", TimeFormatter.formatToKoreanDuration(report.totalMillis), emphasized = true,
                caption = if (hasRecords) "이 날짜에 시작한 기록 기준" else "이 날짜에 시작한 기록 없음"
            )
            report.highest?.let { highest ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ReportField("최고 기록", reportRecordTitle(highest))
            }
            report.lowest?.let { lowest ->
                ReportField("최저 기록", reportRecordTitle(lowest))
            }
        }
        if (hasRecords) {
            ReportCard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("이 날짜에 시작한 기록", style = MaterialTheme.typography.titleMedium)
                    Text("시각은 학습 화면을 연 때부터 저장 때까지예요", style = MaterialTheme.typography.labelMedium)
                }
                report.records.forEachIndexed { index, record ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    key(date, record.potId, record.logId) {
                        ReportRecordRow(record)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRecordRow(record: ReportRecord) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(reportRecordTitle(record), style = MaterialTheme.typography.titleMedium)
        Text(
            "${TimeFormatter.formatToTimeOnly(record.startedAt)}~${TimeFormatter.formatToTimeOnly(record.endedAt)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
