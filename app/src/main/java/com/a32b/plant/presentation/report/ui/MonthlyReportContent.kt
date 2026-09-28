package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.a32b.plant.domain.model.MonthlyReport
import kotlin.math.floor

@Composable
internal fun MonthlyReportContent(report: MonthlyReport) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("${report.month.year}년 ${report.month.monthValue}월", style = MaterialTheme.typography.titleLarge)
        ReportCard(spacing = 12.dp) {
            ReportField("총 공부 시간", reportDuration(report.totalMillis), emphasized = true, caption = "저장일 기준")
            ReportField(
                "하루 평균", reportAverageDuration(report.averageMillis), emphasized = true,
                caption = reportAverageCaption(report)
            )
        }
        ReportCard {
            Text("날짜별 공부 시간", style = MaterialTheme.typography.titleMedium)
            if (report.potComparison.highest == null) Text("이 달에 저장된 기록 없음", style = MaterialTheme.typography.bodyMedium)
            MonthlyDailyGraph(report)
        }
        // 이 달에 저장 기록이 없으면 최고·최저 카드는 숨긴다(일간과 같은 규칙, "기록 없음"은 그래프 카드에서 한 번만).
        if (report.potComparison.highest != null) {
            ReportCard(spacing = 12.dp) {
                ReportField("가장 많이 공부한 화분", reportPotRank(report.potComparison.highest))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                ReportField(
                    "가장 적게 공부한 화분",
                    if (report.potComparison.lowest == null) "비교할 다른 화분 없음"
                    else reportPotRank(report.potComparison.lowest)
                )
            }
        }
    }
}

@Composable
private fun MonthlyDailyGraph(report: MonthlyReport) {
    val upper = reportAxisUpperMillis(report.dailyTotals.maxOfOrNull { it.totalMillis } ?: 0L)
    val gridColor = MaterialTheme.colorScheme.outline
    val barColor = MaterialTheme.colorScheme.tertiary
    val tickLabels = (0..4).map { reportAxisTimeLabel(upper * it / 4.0) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.bodySmall
    val density = LocalDensity.current
    val labelWidth = with(density) {
        tickLabels.maxOf { textMeasurer.measure(it, labelStyle).size.width }.toDp()
    }
    val axisWidth = labelWidth + 8.dp
    val labelHeight = with(density) { textMeasurer.measure("30", labelStyle).size.height.toDp() }
    val graphHeight = maxOf(200.dp, labelHeight * 6 + 16.dp)
    val plotTop = labelHeight / 2
    val plotBottom = graphHeight - labelHeight - 8.dp
    val plotHeight = plotBottom - plotTop
    val voiceDescription = report.dailyTotals.joinToString(", ", prefix = "날짜별 공부 시간. ") {
        "${it.date.dayOfMonth}일 ${reportDuration(it.totalMillis)}"
    }
    val labels = (listOf(1, 5, 10, 15, 20, 25, report.month.lengthOfMonth())
        .filter { it <= report.month.lengthOfMonth() }).distinct()
    val dayLabelWidth = with(density) {
        labels.maxOf { textMeasurer.measure(it.toString(), labelStyle).size.width }.toDp()
    } + 2.dp

    BoxWithConstraints(Modifier.fillMaxWidth().height(graphHeight).semantics { contentDescription = voiceDescription }) {
        val plotWidth = maxWidth - axisWidth
        val count = report.dailyTotals.size
        Canvas(Modifier.fillMaxWidth().height(graphHeight)) {
            val left = axisWidth.toPx()
            val plotHeightPx = plotHeight.toPx()
            val bottom = plotBottom.toPx()
            for (tick in 0..4) {
                val y = bottom - tick * plotHeightPx / 4f
                drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val slot = (size.width - left) / count
            report.dailyTotals.forEachIndexed { index, daily ->
                if (daily.totalMillis > 0L) {
                    val barHeight = (daily.totalMillis.toDouble() / upper * plotHeightPx).toFloat()
                    val barWidth = (slot - 2.dp.toPx()).coerceAtLeast(1.dp.toPx())
                    drawRect(
                        color = barColor,
                        topLeft = Offset(left + slot * index + (slot - barWidth) / 2f, bottom - barHeight),
                        size = Size(barWidth, barHeight)
                    )
                }
            }
        }
        for (tick in 0..4) {
            Text(
                tickLabels[tick],
                modifier = Modifier.offset(y = plotBottom - plotHeight * (tick / 4f) - labelHeight / 2).width(labelWidth),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        labels.forEach { day ->
            val slot = plotWidth / count
            // coerceIn은 폭이 좁아 하한 > 상한이 되면 예외라서 나눠 호출한다(이때 라벨은 왼쪽 경계에 겹침).
            val x = (axisWidth + slot * (day - 0.5f) - dayLabelWidth / 2)
                .coerceAtMost(maxWidth - dayLabelWidth)
                .coerceAtLeast(axisWidth)
            Text(
                day.toString(),
                modifier = Modifier.offset(x = x, y = plotBottom + 8.dp).width(dayLabelWidth),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
}

// 1시간 아래는 20분·40분(눈금 4칸이 5분·10분으로 떨어짐), 1시간부터는 1·2·5 단위로 올림.
// 최소 상한 20분: 짧은 기록만 있는 달도 그래프가 비어 보이지 않게, 17초 같은 값이 꽉 차 보이지도 않게.
// 그래프는 표시용 Double을 쓰고 원본 합계는 Long으로 유지한다.
internal fun reportAxisUpperMillis(maxDailyMillis: Long): Double {
    if (maxDailyMillis <= 1_200_000L) return 1_200_000.0
    if (maxDailyMillis <= 2_400_000L) return 2_400_000.0
    val minimum = 3_600_000.0
    val target = maxOf(minimum, maxDailyMillis.toDouble())
    var step = minimum
    while (step * 10.0 < target) step *= 10.0
    return listOf(step, step * 2.0, step * 5.0, step * 10.0).first { it >= target }
}

internal fun reportAxisTimeLabel(millis: Double): String {
    val totalMinutes = floor(millis / 60_000.0).toLong()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        totalMinutes == 0L -> "0"
        hours == 0L -> "${minutes}분"
        minutes == 0L -> "${hours}시간"
        else -> "${hours}시간 ${minutes}분"
    }
}
