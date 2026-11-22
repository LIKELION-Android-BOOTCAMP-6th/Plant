package com.a32b.plant.presentation.community.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a32b.plant.core.extension.isCompletedAt
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.presentation.core.component.StudyGoalItem

@Composable
fun StudyLogCard(studyLogs: List<StudyLog>) {
    Column {
        studyLogs.forEach { log ->
            Card(shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start= 10.dp, end = 10.dp, top = 3.dp, bottom = 3.dp).fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text("${log.title} [${TimeFormatter.formatToDigitalClock(log.studyingTime)}]", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(10.dp)
                )
                log.contents.forEachIndexed { index, content ->
                    StudyGoalItem(
                        text = content,
                        isCompleted = log.completedContents.isCompletedAt(index),
                        modifier = Modifier.padding(10.dp),
                        // 화분 상세의 학습 기록 상세(StudyLogDetailDialog)와 같은 작은 글씨
                        style = MaterialTheme.typography.bodySmall,
                        iconSize = 8.sp
                    )
                }
            }
        }
    }
}
