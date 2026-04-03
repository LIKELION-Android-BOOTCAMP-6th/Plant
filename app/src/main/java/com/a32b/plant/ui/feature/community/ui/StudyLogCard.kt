package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.ui.theme.Typography

@Composable
fun StudyLogCard(studyLogs: List<StudyLog>) {
    Column {
        studyLogs.forEach { log ->
            Card(shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(start= 10.dp, end = 10.dp, top = 3.dp, bottom = 3.dp).fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(2.dp),
//                colors = CardDefaults.cardColors(Color.White)) {
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Text("${log.title} [${TimeFormatter.formatToDigitalClock(log.studyingTime)}]", style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(10.dp),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                log.contents.forEach { content ->
                    Text(content, style = Typography.bodyMedium, fontSize = 13.sp,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}