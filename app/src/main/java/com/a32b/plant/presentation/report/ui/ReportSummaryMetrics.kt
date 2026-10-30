package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a32b.plant.R
import com.a32b.plant.core.util.TimeFormatter

@Composable
fun ReportSummaryMetrics(
    totalMillis: Long,
    studyCount: Int,
    streakDays: Int,
    streakLabel: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_total_time,
                label = "총 시간",
                value = TimeFormatter.formatToDigitalClock(totalMillis),
                spokenValue = TimeFormatter.formatToKoreanDuration(totalMillis),
                modifier = Modifier.weight(1f)
            )
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_study_count,
                label = "학습 횟수",
                value = "${studyCount}회",
                spokenValue = "${studyCount}회",
                modifier = Modifier.weight(1f)
            )
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_streak,
                label = streakLabel,
                value = "${streakDays}일",
                spokenValue = "${streakDays}일",
                modifier = Modifier.weight(1f)
            )
        }
        Text("연속은 1분 이상 공부한 날만 집계됩니다", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ReportSummaryMetricChip(
    iconRes: Int,
    label: String,
    value: String,
    spokenValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$label, $spokenValue"
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = reportCardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            val valueStyle = MaterialTheme.typography.titleMedium
            BasicText(
                text = value,
                style = valueStyle.copy(textAlign = TextAlign.Center),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = valueStyle.fontSize)
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
