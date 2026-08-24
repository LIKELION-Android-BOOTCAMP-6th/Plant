package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.R
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.viewmodel.StudyLogUi
import com.a32b.plant.presentation.theme.Typography
import com.a32b.plant.presentation.theme.sub3

@Composable
fun StudyingGoalCheckDialog(
    mode: StudyingGoalCheckMode,
    studyLog: List<StudyLogUi>,
    onEdit: () -> Unit = {},
    onCompleted: (index: Int, isCompleted: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val showEditButton = mode != StudyingGoalCheckMode.INTERRUPTED
    val showActionRow = mode != StudyingGoalCheckMode.CHECK
    Dialog(onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "학습 목표",
                        style = Typography.titleSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    if(showEditButton){
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = "edit",
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                if(studyLog.isEmpty()){
                    Text("아직 학습 목표를 설정하지 않았어요.", style = Typography.bodyMedium)
                    Spacer(Modifier.height(7.dp))
                }else{
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(studyLog){ index, item ->
                            StudyLogItem(
                                isInterrupted = !showEditButton,
                                log = item,
                                onCompleted = {onCompleted(index, it)}
                            )
                        }
                    }
                }

                if (showActionRow){
                    Row(modifier = Modifier.fillMaxWidth().padding(7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = sub3, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) { Text("취소", style = Typography.bodySmall)}

                        Button(
                            onClick = onConfirm,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.7f), contentColor = MaterialTheme.colorScheme.primary)
                        ) { Text( if(showEditButton) "종료" else "저장", style = Typography.bodySmall)}
                    }
                }
            }
        }
    }
}

@Composable
fun StudyLogItem(isInterrupted: Boolean, log: StudyLogUi, onCompleted: (Boolean) -> Unit ){
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable{onCompleted(!log.isCompleted)}
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isInterrupted) {
            Checkbox(checked = log.isCompleted, onCheckedChange = onCompleted)
            Spacer(modifier = Modifier.width(7.dp))
        }
        Text(
            text = log.log,
            style = Typography.bodyMedium,
            color = if (log.isCompleted) Color.LightGray else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (log.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )


    }
}
