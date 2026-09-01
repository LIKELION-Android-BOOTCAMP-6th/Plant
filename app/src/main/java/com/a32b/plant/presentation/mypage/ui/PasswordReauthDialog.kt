package com.a32b.plant.presentation.mypage.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.presentation.theme.fontColor
import com.a32b.plant.presentation.theme.fontColorSub
import com.a32b.plant.presentation.theme.primary
import com.a32b.plant.presentation.theme.sub2
import com.a32b.plant.presentation.theme.textFieldBackground

@Composable
fun PasswordReauthDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "본인 확인",
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "탈퇴를 위해 비밀번호를 입력해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text("비밀번호", style = MaterialTheme.typography.bodySmall)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = textFieldBackground,
                        unfocusedContainerColor = textFieldBackground,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = fontColor,
                        unfocusedTextColor = fontColor,
                        focusedPlaceholderColor = fontColorSub,
                        unfocusedPlaceholderColor = fontColorSub
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(sub2)
                    ) {
                        Text("취소", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.size(10.dp))

                    Button(
                        onClick = { if (password.isNotBlank()) onConfirm(password) },
                        modifier = Modifier
                            .height(30.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(primary)
                    ) {
                        Text("확인", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
