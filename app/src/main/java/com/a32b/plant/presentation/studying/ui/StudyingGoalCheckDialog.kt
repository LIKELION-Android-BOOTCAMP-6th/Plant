package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.plantCheckboxColors
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.StudyingConstants.MAX_STUDY_LOG_SIZE
import com.a32b.plant.presentation.studying.StudyingConstants.STUDY_LOG_LIMIT_STATE_DESCRIPTION
import com.a32b.plant.presentation.studying.viewmodel.StudyLogUi


@Composable
fun StudyingGoalCheckDialog(
    mode: StudyingGoalCheckMode,
    studyLog: List<StudyLogUi>,
    onEdit: (Int) -> Unit,
    onCompleted: (index: Int, isCompleted: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (mode == StudyingGoalCheckMode.FINISH) "학습 종료" else "학습 목표",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if(studyLog.isEmpty()){
                    Spacer(Modifier.height(4.dp))
                    Text("아직 학습 목표를 설정하지 않았어요.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(7.dp))
                }else{
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(studyLog){ index, item ->
                            StudyLogItem(
                                log = item,
                                onEdit = { onEdit(index) },
                                onCompleted = {onCompleted(index, it)}
                            )
                            if (index < studyLog.lastIndex){
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 13.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // 최대 개수면 비활성처럼만 표시 — enabled=false면 클릭이 막혀 안내 토스트를 띄울 수 없으므로 클릭은 ViewModel 가드에서 처리
                val canAddLog = studyLog.size < MAX_STUDY_LOG_SIZE
                TextButton(
                    onClick = { onEdit(-1) },
                    modifier = Modifier
                        .alpha(if (canAddLog) 1f else 0.38f)
                        // alpha는 시각 표시뿐이라 TalkBack에 상태를 따로 알림
                        // disabled()는 클릭 동작이 막혀 토스트를 못 띄울 수 있고, contentDescription은 버튼 이름을 덮어써서 쓰지 않음
                        .semantics { if (!canAddLog) stateDescription = STUDY_LOG_LIMIT_STATE_DESCRIPTION }
                ) {
                    Text(
                        "추가하기",
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = TextDecoration.Underline
                    )
                }

                when (mode) {
                    StudyingGoalCheckMode.FINISH -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(7.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DialogSmallButton(
                                text = stringResource(R.string.common_cancel),
                                containerColor = MaterialTheme.colorScheme.outline,
                                onClick = onDismiss
                            )
                            Spacer(Modifier.width(8.dp))
                            DialogSmallButton(
                                text = "종료",
                                containerColor = MaterialTheme.colorScheme.primary.copy(0.7f),
                                onClick = onConfirm
                            )
                        }
                    }
                    StudyingGoalCheckMode.CHECK -> {
                        DialogSmallButton(
                            text = stringResource(R.string.common_close),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 7.dp, bottom = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 다이얼로그 하단의 작은 버튼 — 최소 33dp(글꼴 1.0에서는 지금과 같음), 글자가 더 높아지면 늘어남 (#673)
 *
 * - 고정 height(33.dp)는 큰 글꼴에서 글자를 잘라서 heightIn(min)으로 바꿈
 * - Material3 Button은 높이가 고정되지 않으면 차지하는 자리를 48dp로 넓혀(minimumInteractiveComponentSize)
 *   글꼴 1.0에서도 다이얼로그가 길어지므로 자리 넓히기만 끔
 *   (터치 영역은 Compose가 48dp보다 작은 터치 대상을 넓혀 판정함 — ui NodeCoordinator)
 */
@Composable
internal fun DialogSmallButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            modifier = modifier.heightIn(min = 33.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor)
        ) { Text(text, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun StudyLogItem(log: StudyLogUi, onEdit: () -> Unit, onCompleted: (Boolean) -> Unit ){
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable{onCompleted(!log.isCompleted)}
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = log.isCompleted,
            onCheckedChange = onCompleted,
            colors = plantCheckboxColors()
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = log.log,
            style = MaterialTheme.typography.bodyMedium,
            color = if (log.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (log.isCompleted) TextDecoration.LineThrough else TextDecoration.None
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onEdit,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "edit",
                modifier = Modifier.size(17.dp).padding(end = 5.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
