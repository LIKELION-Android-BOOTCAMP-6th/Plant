package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.Typography
import com.a32b.plant.presentation.theme.sub3

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
    val maxLogSize = 10

    val localLogs = remember(studyLogs) {
        mutableStateListOf<String>().apply {
            if (studyLogs.isEmpty()) add("") else addAll(studyLogs)
        }
    }
    var index by remember { mutableIntStateOf(initialIndex) }
    val currentText = localLogs.getOrElse(index) { "" }

    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(21.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
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
                modifier = Modifier.padding(top = 10.dp, bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text("[$tag] $title", style = Typography.titleSmall)

                Spacer(Modifier.height(7.dp))

                if (!isEditing){
                    Text(
                        "${index + 1} / 10 ",
                        style = Typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
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
                                if (index > 0) index--
                            },
                            enabled = index > 0
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
                                    style = Typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            },
                            maxLines = 11,
                            textStyle = Typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            "${currentText.length}/$maxLength",
                            style = Typography.bodySmall,
                            color = if (currentText.length >= maxLength) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                if (index < 10) index++
                            },
                            enabled = index < localLogs.size - 1
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isEditing){
                                onDismiss()
                            }else if (!isEditing && localLogs.size < maxLogSize){
                                localLogs.add("")
                                index = localLogs.size - 1
                            }
                        },
                        enabled = localLogs.size < maxLogSize || isEditing,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(33.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = sub3, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text(if (isEditing) "취소" else "추가", style = Typography.bodySmall) }

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
                        modifier = Modifier.height(33.dp).padding(start = 10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(0.3f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) { Text("삭제", style = Typography.bodySmall) }


                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {if (isEditing) onConfirmEdit(localLogs[index]) else onConfirm(localLogs.toList())},
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(33.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(0.7f), contentColor = MaterialTheme.colorScheme.primary)
                    ) { Text("완료", style = Typography.bodySmall)}

                }

                if (!isEditing){
                    TextButton(onClick = onDismiss) {
                        Text(
                            "다음에 입력하기",
                            style = Typography.bodySmall,
                            color = Color.Gray,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
