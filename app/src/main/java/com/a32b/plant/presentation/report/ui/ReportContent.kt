package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.a32b.plant.R
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.report.viewmodel.ReportTab
import com.a32b.plant.presentation.report.viewmodel.ReportUiState
import java.time.LocalDate

@Composable
internal fun ReportContent(
    state: ReportUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onTabSelected: (ReportTab) -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("리포트", style = MaterialTheme.typography.displayLarge)
        ReportCalendar(
            state = state,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDateSelected = onDateSelected
        )
        ReportTabs(state.tab, onTabSelected)

        when {
            state.isLoading -> LoadingBox(Modifier.fillMaxWidth().height(200.dp))
            state.loadError -> ReportFailure("기록을 불러오지 못했어요", onRetry)
            state.calculationError -> ReportFailure("기록의 공부 시간이 올바르지 않아 통계를 계산할 수 없어요")
            state.tab == ReportTab.DAILY -> {
                val report = state.dailyReport
                if (report != null) DailyReportContent(report)
            }
            else -> {
                val report = state.monthlyReport
                if (report != null) MonthlyReportContent(report)
            }
        }
    }
}

@Composable
private fun ReportCalendar(
    state: ReportUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val month = state.month
    val firstWeekday = month.atDay(1).dayOfWeek.value % 7
    val weekCount = (firstWeekday + month.lengthOfMonth() + 6) / 7
    val weekdayNames = listOf("일", "월", "화", "수", "목", "금", "토")
    val canGoNext = state.canGoNext

    ReportCard(spacing = 0.dp, padding = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_left),
                    contentDescription = "이전 달",
                    modifier = Modifier.size(23.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text("${month.year}년 ${month.monthValue}월", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNextMonth, enabled = canGoNext, modifier = Modifier.size(48.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_right),
                    contentDescription = "다음 달",
                    modifier = Modifier.size(23.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoNext) 1f else 0.38f)
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weekdayNames.forEach { name ->
                Text(
                    name,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        repeat(weekCount) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val day = week * 7 + weekday - firstWeekday + 1
                    Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            ReportCalendarDate(
                                date = date,
                                isToday = date == state.today,
                                isSelected = date == state.selectedDate,
                                hasRecord = date in state.calendarDates,
                                onClick = { onDateSelected(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCalendarDate(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit
) {
    val circleColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    val circleBorder = if (isToday && !isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    val description = buildString {
        append("${date.year}년 ${date.monthValue}월 ${date.dayOfMonth}일")
        if (isToday) append(", 오늘")
        if (isSelected) append(", 선택됨")
        append(if (hasRecord) ", 기록 있음" else ", 기록 없음")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circleColor)
                .then(if (circleBorder != null) Modifier.border(circleBorder, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
            )
        }
        Box(Modifier.height(8.dp), contentAlignment = Alignment.Center) {
            if (hasRecord) Box(Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
        }
    }
}

@Composable
private fun ReportTabs(selected: ReportTab, onTabSelected: (ReportTab) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportTabButton("일간", selected == ReportTab.DAILY, Modifier.weight(1f)) { onTabSelected(ReportTab.DAILY) }
        ReportTabButton("월간", selected == ReportTab.MONTHLY, Modifier.weight(1f)) { onTabSelected(ReportTab.MONTHLY) }
    }
}

@Composable
private fun ReportTabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = if (selected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified)
    }
}

@Composable
private fun ReportFailure(message: String, onRetry: (() -> Unit)? = null) {
    ReportCard {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) {
            Button(onClick = onRetry, modifier = Modifier.height(48.dp)) {
                Text("다시 시도", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
internal fun ReportField(label: String, value: String, emphasized: Boolean = false, caption: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium)
        if (caption != null) Text(caption, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun ReportCard(
    spacing: Dp = 8.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}

@Composable
internal fun ReportPeriodTitle(date: LocalDate) {
    Text(TimeFormatter.formatWithDayOfWeek(date.atStartOfDay()), style = MaterialTheme.typography.titleLarge)
}
