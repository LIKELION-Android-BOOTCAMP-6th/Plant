package com.a32b.plant.presentation.mypage.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.a32b.plant.presentation.mypage.viewmodel.DeleteAccountEvent
import com.a32b.plant.presentation.mypage.viewmodel.DeleteAccountViewModel
import com.a32b.plant.presentation.theme.LocalIsDarkTheme
import com.a32b.plant.presentation.theme.deleteAccountButtonContentDark
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountManageScreen(
    navController: NavController,
    deleteViewModel: DeleteAccountViewModel = hiltViewModel()
) {
    val deleteUiState by deleteViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)
    val credentialManager = remember { CredentialManager.create(context) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteSecondConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deleteViewModel.events.collect { event ->
            when (event) {
                is DeleteAccountEvent.ShowToast -> context.showToast(event.message)

                is DeleteAccountEvent.NavigateToSignIn ->
                    navController.navigate(Routes.SignIn) {
                        popUpTo(0) { inclusive = true }
                    }

                is DeleteAccountEvent.OpenGoogleAccountSettings -> {
                    val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                    intent.putExtra("account_types", arrayOf("com.google"))
                    try {
                        context.startActivity(intent)
                    } catch (error: Exception) {
                        context.showToast("계정 추가 설정을 열 수 없습니다.")
                    }
                }

                is DeleteAccountEvent.RequestGoogleReauth -> {
                    coroutineScope.launch {
                        context.showToast(
                            "보안을 위해 Google 계정을\n다시 확인합니다.",
                            Toast.LENGTH_LONG
                        )
                        val googleIdOption =
                            GetSignInWithGoogleOption.Builder(webClientId).build()
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
                            deleteViewModel.reauthenticateWithGoogleAndDelete(idToken)
                        } catch (error: GetCredentialCancellationException) {
                            deleteViewModel.cancelLoading()
                        } catch (error: NoCredentialException) {
                            deleteViewModel.cancelLoading()
                            context.showToast("기기에 등록된 Google 계정이 없습니다.")
                            deleteViewModel.openGoogleAccountSettings()
                        } catch (error: CancellationException) {
                            deleteViewModel.cancelLoading()
                            throw error
                        } catch (error: Exception) {
                            deleteViewModel.cancelLoading()
                            context.showToast("구글 재인증에 실패했습니다. 다시 시도해주세요.")
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            text = if (isDeleteSecondConfirm) "정말로 탈퇴하시겠습니까?" else "탈퇴 하시겠습니까?",
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
                    deleteViewModel.requestDeleteAccount()
                } else {
                    isDeleteSecondConfirm = true
                }
            }
        )
    }

    if (deleteUiState.isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            LoadingBox(modifier = Modifier.size(80.dp))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = 0.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text("계정 관리", style = MaterialTheme.typography.displayLarge)
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        enabled = !deleteUiState.isLoading
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ButtonTemplate(
                text = "회원탈퇴",
                enabled = !deleteUiState.isLoading,
                textColor = if (LocalIsDarkTheme.current) {
                    deleteAccountButtonContentDark
                } else {
                    MaterialTheme.colorScheme.error
                },
                onClick = {
                    isDeleteSecondConfirm = false
                    showDeleteDialog = true
                }
            )
        }
    }
}
