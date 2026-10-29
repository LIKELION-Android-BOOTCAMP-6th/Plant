package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.R
import com.a32b.plant.presentation.studying.StudyingConstants.MAX_STUDY_LOG_SIZE

@Composable
fun StudyingGoalInputDialog(
    isEditing: Boolean = false,
    initialIndex: Int = 0,
    tag: String,
    title: String,
    studyLogs: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    onConfirmEdit: (String) -> Unit = {}
) {
    val maxLength = 100
    val maxLogSize = MAX_STUDY_LOG_SIZE

    val localLogs = remember(studyLogs) {
        mutableStateListOf<String>().apply {
            if (studyLogs.isEmpty()) add("") else addAll(studyLogs)
        }
    }
    // localLogs가 새로 만들어지면 index도 새 목록 범위 안으로 다시 맞춤 (범위 밖 index로 쓰기 시 크래시 방지)
    var index by remember(localLogs) { mutableIntStateOf(initialIndex.coerceIn(0, localLogs.lastIndex)) }
    val currentText = localLogs.getOrElse(index) { "" }
    val canGoPrev = index > 0
    val canGoNext = index < localLogs.lastIndex

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if(!isEditing)0.5f else 0.3f)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Column(
                modifier = Modifier.padding(top = 10.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text("[$tag] $title", style = MaterialTheme.typography.titleSmall)

                Spacer(Modifier.height(7.dp))

                if (!isEditing){
                    Text(
                        "${index + 1} / $maxLogSize",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center

                ) {
                    if (!isEditing){
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                if (canGoPrev) index--
                            },
                            enabled = canGoPrev,
                            // 자리는 유지해야 입력창 너비가 변하지 않으므로 숨김은 alpha로 처리
                            // 숨김 상태에서는 TalkBack 포커스도 가지 않도록 semantics 제거
                            modifier = Modifier
                                .alpha(if (canGoPrev) 1f else 0f)
                                .then(if (canGoPrev) Modifier else Modifier.clearAndSetSemantics {})
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_left),
                                contentDescription = "왼쪽",
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }

                    if (isEditing) Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = currentText,
                            shape = RoundedCornerShape(10.dp),
                            onValueChange = { newValue ->
                                if (newValue.length <= maxLength) {
                                    localLogs[index] = newValue
                                }
                            },
                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
                            placeholder = {
                                Text(
                                    text = "오늘의 학습 목표를 기록해보세요!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            },
                            maxLines = 11,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            "${currentText.length}/$maxLength",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentText.length >= maxLength) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 8.dp)
                        )

                    }
                    if (isEditing) Spacer(Modifier.width(10.dp))


                    if (!isEditing){
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                if (canGoNext) index++
                            },
                            enabled = canGoNext,
                            modifier = Modifier
                                .alpha(if (canGoNext) 1f else 0f)
                                .then(if (canGoNext) Modifier else Modifier.clearAndSetSemantics {})
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_right),
                                contentDescription = "오른쪽",
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 13.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEditing){
                        Button(
                            onClick = {
                                if (localLogs.size < maxLogSize){
                                    localLogs.add("")
                                    index = localLogs.size - 1
                                }
                            },
                            enabled = localLogs.size < maxLogSize,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.outline,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("추가", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        // 편집 모드는 "추가"가 없으므로 빈 칸으로 채워 버튼 너비를 4개일 때와 맞추고 오른쪽 정렬
                        Spacer(Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            if (!isEditing){
                                if (localLogs.size > 1) {
                                    localLogs.removeAt(index)
                                    if (index >= localLogs.size) index = localLogs.size - 1
                                } else {
                                    localLogs[0] = ""
                                }
                            } else{
                                localLogs[index] = ""
                            }

                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(0.3f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("삭제", style = MaterialTheme.typography.bodySmall) }

                    Button(
                        onClick = {if (isEditing) onConfirmEdit(localLogs[index]) else onConfirm(localLogs.toList())},
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("완료", style = MaterialTheme.typography.bodySmall)}

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(38.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("닫기", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
