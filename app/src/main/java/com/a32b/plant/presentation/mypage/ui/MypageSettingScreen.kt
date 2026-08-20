package com.a32b.plant.presentation.mypage.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.mypage.viewmodel.MyPageSettingEvent
import com.a32b.plant.presentation.mypage.viewmodel.MyPageSettingViewModel
import com.a32b.plant.presentation.theme.fontColor
import com.a32b.plant.presentation.theme.fontColorSub
import com.a32b.plant.presentation.theme.primary
import com.a32b.plant.presentation.theme.sub2
import com.a32b.plant.presentation.theme.textFieldBackground
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun MyPageSettingScreen(
    navController: NavController,
    viewModel: MyPageSettingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val webClientId = stringResource(R.string.default_web_client_id)
    val credentialManager = remember { CredentialManager.create(context) }

    // 회원탈퇴 2단계 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteSecondConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyPageSettingEvent.ShowToast ->
                    context.showToast(event.message)

                is MyPageSettingEvent.NavigateToSignIn ->
                    navController.navigate(Routes.SignIn) {
                        popUpTo(0) { inclusive = true }
                    }

                is MyPageSettingEvent.RequestGoogleReauth -> {
                    // 구글 재인증: Credential Manager로 idToken 획득 후 ViewModel에 전달
                    coroutineScope.launch {
                        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        try {
                            val result = credentialManager.getCredential(
                                request = request,
                                context = context as Activity
                            )
                            val idToken = GoogleIdTokenCredential
                                .createFrom(result.credential.data).idToken
                            viewModel.reauthenticateWithGoogleAndDelete(idToken)
                        } catch (e: GetCredentialCancellationException) {
                            // 사용자가 취소 — 아무것도 하지 않음
                        } catch (e: NoCredentialException) {
                            context.showToast("기기에 등록된 Google 계정이 없습니다.")
                            val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                            intent.putExtra("account_types", arrayOf("com.google"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            context.showToast("구글 재인증에 실패했습니다. 다시 시도해주세요.")
                        }
                    }
                }
            }
        }
    }

    // 2단계 확인 다이얼로그
    if (showDeleteDialog) {
        ConfirmDialog(
            text = if (isDeleteSecondConfirm) "정말로 탈퇴하시겠습니까?"
            else "탈퇴 하시겠습니까?",
            semiText = if (isDeleteSecondConfirm) "탈퇴 시 모든 학습 기록이 삭제되며 복구할 수 없습니다."
            else "계정을 삭제하시려면 '예'를 눌러주세요.",
            onDismiss = {
                showDeleteDialog = false
                isDeleteSecondConfirm = false
            },
            onConfirm = {
                if (isDeleteSecondConfirm) {
                    showDeleteDialog = false
                    isDeleteSecondConfirm = false
                    viewModel.requestDeleteAccount()
                } else {
                    isDeleteSecondConfirm = true
                }
            }
        )
    }

    // 비밀번호 재인증 다이얼로그 (이메일 유저)
    if (uiState.showPasswordDialog) {
        PasswordReauthDialog(
            onDismiss = { viewModel.dismissPasswordDialog() },
            onConfirm = { password -> viewModel.reauthenticateWithEmailAndDelete(password) }
        )
    }

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "앱 설정",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
//            ButtonTemplate(text = "이용약관") { }
//            ButtonTemplate(text = "FAQ") { }
//            ButtonTemplate(text = "사용설명서") { }
//            ButtonTemplate(text = "앱 테마") { }

                ButtonTemplate(
                    text = "회원탈퇴",
                    enabled = !uiState.isLoading
                ) {
                    isDeleteSecondConfirm = false
                    showDeleteDialog = true
                }
            }

            if (uiState.isLoading) {
                LoadingBox()
            }
        }
    }
}

@Composable
private fun PasswordReauthDialog(
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
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "탈퇴를 위해 비밀번호를 입력해주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
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
