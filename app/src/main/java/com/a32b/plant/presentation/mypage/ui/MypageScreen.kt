package com.a32b.plant.presentation.mypage.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.mypage.viewmodel.DeleteAccountEvent
import com.a32b.plant.presentation.mypage.viewmodel.DeleteAccountViewModel
import com.a32b.plant.presentation.mypage.viewmodel.MyPageEvent
import com.a32b.plant.presentation.mypage.viewmodel.MyPageUiState
import com.a32b.plant.presentation.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.presentation.theme.PlantTheme
import com.a32b.plant.presentation.theme.Typography
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun MyPageScreen(navController: NavController, viewModel: MyPageViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    // 로그아웃 확인 다이얼로그 상태
    var showLogoutDialog by remember { mutableStateOf(false) }
    // 프로필 수정 다이얼로그 상태
    var showProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // 회원탈퇴
    val deleteViewModel: DeleteAccountViewModel = hiltViewModel()
    val deleteUiState by deleteViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)
    val credentialManager = remember { CredentialManager.create(context) }

    // 회원탈퇴 2단계 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteSecondConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            context.showToast("업데이트 완료")
            showProfileDialog = false
            viewModel.clearProfileState()
        }
    }

    PlantTheme(darkTheme = uiState.isDarkMode) {
        // 로그아웃
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is MyPageEvent.ShowToast ->
                        context.showToast(event.message)

                    is MyPageEvent.NavigateToSignIn ->
                        navController.navigate(Routes.SignIn) {
                            popUpTo(0) { inclusive = true }
                        }
                }
            }
        }

        // 회원탈퇴 이벤트
        LaunchedEffect(Unit) {
            deleteViewModel.events.collect { event ->
                when (event) {
                    is DeleteAccountEvent.ShowToast ->
                        context.showToast(event.message)

                    is DeleteAccountEvent.NavigateToSignIn ->
                        navController.navigate(Routes.SignIn) {
                            popUpTo(0) { inclusive = true }
                        }

                    is DeleteAccountEvent.RequestGoogleReauth -> {
                        // 구글 재인증: Credential Manager로 idToken 획득 후 ViewModel에 전달
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
                            } catch (e: GetCredentialCancellationException) {
                                // 사용자가 취소 — 로딩 해제 후 복귀
                                deleteViewModel.cancelLoading()
                            } catch (e: NoCredentialException) {
                                deleteViewModel.cancelLoading()
                                context.showToast("기기에 등록된 Google 계정이 없습니다.")
                                val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                                intent.putExtra("account_types", arrayOf("com.google"))
                                context.startActivity(intent)
                            } catch (e: CancellationException) {
                                deleteViewModel.cancelLoading()
                                throw e
                            } catch (e: Exception) {
                                deleteViewModel.cancelLoading()
                                context.showToast("구글 재인증에 실패했습니다. 다시 시도해주세요.")
                            }
                        }
                    }
                }
            }
        }


        // 로그아웃 확인 다이얼로그
        if (showLogoutDialog) {
            ConfirmDialog(
                text = "로그아웃 하시겠습니까?",
                onDismiss = {
                    showLogoutDialog = false
                    viewModel.clearProfileState()
                },
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout()
                }
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                onDismiss = {
                    viewModel.clearProfileState()
                    showProfileDialog = false
                },
                uiState = uiState,
                viewModel = viewModel
            )
        }
        // 회원탈퇴 2단계 확인 다이얼로그
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
                        deleteViewModel.requestDeleteAccount()
                    } else {
                        isDeleteSecondConfirm = true
                    }
                }
            )
        }

        // 비밀번호 재인증 다이얼로그 (이메일 유저)
        if (deleteUiState.showPasswordDialog) {
            PasswordReauthDialog(
                onDismiss = { deleteViewModel.dismissPasswordDialog() },
                onConfirm = { password ->
                    deleteViewModel.reauthenticateWithEmailAndDelete(password)
                }
            )
        }

        // 탈퇴 진행 중 화면 이탈 차단
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

        LoadableScreen(viewModel) {
            MyPageContent(
                uiState = uiState,
                onProfileClick = {
                    viewModel.getImageLevelList()
                    showProfileDialog = true
                },
                onDarkModeToggle = { isDarkMode ->
                    viewModel.updateDarkMode(isDarkMode)
                },
                onGuideClick = {
                    context.showToast("준비 중입니다.")
                },
                onTermsClick = {
                    context.showToast("준비 중입니다.")
                },
                onPrivacyClick = {
                    context.showToast("준비 중입니다.")
                },
                onLogoutClick = {
                    showLogoutDialog = true
                },
                isDeleting = deleteUiState.isLoading,
                onDeleteAccountClick = {
                    isDeleteSecondConfirm = false
                    showDeleteDialog = true
                }
            )
        }

    }
}


@Composable
private fun MyPageContent(
    uiState: MyPageUiState,
    onProfileClick: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onGuideClick: () -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit,
    isDeleting: Boolean,
    onDeleteAccountClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ProfileRow(
                uiState = uiState,
                onProfileClick = onProfileClick
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DividerImage()
                DarkModeToggleButton(
                    isDarkMode = uiState.isDarkMode,
                    isEnabled = !uiState.isDarkModeUpdating,
                    onToggle = onDarkModeToggle
                )
                ButtonTemplate(text = "사용 가이드", onClick = onGuideClick)
                ButtonTemplate(text = "서비스 이용약관", onClick = onTermsClick)
                ButtonTemplate(text = "개인정보처리방침", onClick = onPrivacyClick)
                ButtonTemplate(text = "로그아웃", onClick = onLogoutClick)
                ButtonTemplate(
                    text = "회원탈퇴",
                    enabled = !isDeleting,
                    onClick = onDeleteAccountClick
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyPageContentPreview() {
    PlantTheme {
        MyPageContent(
            uiState = MyPageUiState(
                nickname = "USER",
                profileImg = "1",
                isDarkMode = false,
                totalStudyTime = "4시간 10분"
            ),
            onProfileClick = {},
            onDarkModeToggle = {},
            onGuideClick = {},
            onTermsClick = {},
            onPrivacyClick = {},
            onLogoutClick = {},
            isDeleting = false,
            onDeleteAccountClick = {}
        )
    }
}

@Composable
fun DarkModeToggleButton(
    isDarkMode: Boolean,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,

            ) {
            // 좌측
            Text(
                text = "다크모드",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            // 우측
            Switch(
                checked = isDarkMode,
                modifier = Modifier.scale(0.9f),
                enabled = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
    }
}

@Composable
fun ButtonTemplate(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = text, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ProfileRow(
    uiState: MyPageUiState,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clickable { onProfileClick() }
        ) {

            ProfileImage(
                level = uiState.profileImg.replace("lv.", ""),
                size = 60
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "프로필 수정",

                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1F)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.nickname} 님",
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "총 공부 시간",
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = uiState.totalStudyTime,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DividerImage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_mypage_divider),
            contentDescription = "구분선",
            modifier = Modifier.size(45.dp),
        )
    }
}

// 프로필 편집 다이얼로그 화분 이미지 배치
@Composable
fun SetImages(
    levelList: List<String>,
    selectedImageLevel: String,
    onImageClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        levelList.forEach { level ->
            val removeTextResult = level.replace("lv.", "")
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selectedImageLevel == level) 5.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { onImageClick(level) }
            ) {
                ProfileImage(level = removeTextResult, size = 60)
            }
        }
    }
}


@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    uiState: MyPageUiState,
    viewModel: MyPageViewModel
) {
    // 다이얼로그 안에서만 임시로 쓸 상태들 (입력 중인 값)
    var newUserName by remember { mutableStateOf(uiState.nickname) }
    var selectedImageLevel by remember { mutableStateOf(uiState.profileImg) }

    Dialog(onDismissRequest = {
        viewModel.clearProfileState()
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        value = newUserName,
                        onValueChange = {
                            if (it.length <= 10)
                                newUserName = it
                            viewModel.resetNicknameError()
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        label = { Text("닉네임 변경 (2~10자)", style = Typography.labelSmall) },
                        isError = uiState.nicknameError != null
                    )

                    if (uiState.nicknameError != null) {
                        Text(
                            text = uiState.nicknameError,
                            color = Color.Red,
                            style = Typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                SetImages(
                    levelList = uiState.levelList,
                    selectedImageLevel = selectedImageLevel,
                    onImageClick = { selectedImageLevel = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.clearProfileState()
                            onDismiss()
                        },
                        modifier = Modifier
                            .height(45.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            "취소",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.updateProfile(newUserName, selectedImageLevel)
                        },
                        enabled = !uiState.isLoading, // 작업중이면 버튼 클릭 비활성화 하려고 추가
                        modifier = Modifier
                            .height(45.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("저장", style = Typography.bodyMedium) }
                }
            }
        }
    }
}