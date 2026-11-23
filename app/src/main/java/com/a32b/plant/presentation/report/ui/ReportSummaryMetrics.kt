package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
        // 값이 두 줄이 되는 칩이 있어도 세 칩 높이를 맞춘다 (#649)
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_total_time,
                label = "총 시간",
                value = TimeFormatter.formatToHoursMinutes(totalMillis),
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_study_count,
                label = "학습 횟수",
                value = "${studyCount}회",
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            ReportSummaryMetricChip(
                iconRes = R.drawable.ic_report_streak,
                label = streakLabel,
                value = "${streakDays}일",
                modifier = Modifier.weight(1f).fillMaxHeight()
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
    modifier: Modifier = Modifier
) {
    Card(
        // 화면 글자(value)를 그대로 읽는다 (#649)
        modifier = modifier.clearAndSetSemantics {
            contentDescription = "$label, $value"
        },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = reportCardContainerColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            // 다른 칩이 두 줄이라 이 칩이 높아지면 내용을 세로 가운데에 둔다
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            val valueStyle = MaterialTheme.typography.titleMedium
            BasicText(
                text = keepWordsTogether(value),
                style = valueStyle.copy(textAlign = TextAlign.Center),
                // 큰 글꼴에서 긴 시간이 잘리지 않게 두 줄까지 허용 (#649, 한 줄일 때 360dp·글꼴 2.0에서 "23시간 59분"의 끝 글자가 잘렸음)
                maxLines = 2,
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

/**
 * 띄어쓰기에서만 줄이 나뉘게 단어 안 글자 사이에 WORD JOINER(U+2060)를 넣는다 (#649)
 * 한글은 음절 사이에서도 줄이 나뉘어 "743시간 59" / "분"처럼 쪼개졌음 (에뮬레이터 측정)
 * 화면 표시용 — 읽기 문구(contentDescription)는 원래 값을 쓴다
 */
internal fun keepWordsTogether(text: String): String =
    text.split(' ').joinToString(" ") { word -> word.toList().joinToString(WORD_JOINER) }

private const val WORD_JOINER = "⁠"
