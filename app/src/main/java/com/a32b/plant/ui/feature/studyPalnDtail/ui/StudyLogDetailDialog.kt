package com.a32b.plant.ui.feature.studyPalnDtail.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import java.time.ZoneId

@Composable
fun StudyLogDetailDialog(
    log: StudyLog,
    ondismiss: ()-> Unit
){
    val dateTime = log.createAt.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()

    Dialog(onDismissRequest = ondismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
            ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                //상단 - 제목, 닫기버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (log.title.isNotEmpty()) log.title else "학습기록 상세" ,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = ondismiss,
                        modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close,
                            contentDescription = "닫기")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = fontColorSub, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                //정보
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text(text = TimeFormatter.formatToKoreanDate(dateTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = fontColor
                    )
                    Text(text = "공부시간 : [${TimeFormatter.formatToDigitalClock(log.studyingTime)}]",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = fontColor)
                }
                Spacer(modifier = Modifier.height(16.dp))

                //상세 기록 내용
                if(log.contents.isNotEmpty()){
                    Text(text = "학습 내용",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = fontColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        log.contents.forEach { content ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)){
                                Text(text = "•",
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = fontColor
                                )
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = fontColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    Text(text = "기록된 상세 내용이 없습니다.")
                }
            }
        }
    }
}