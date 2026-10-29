package com.a32b.plant.presentation.auth.ui

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.a32b.plant.presentation.auth.viewmodel.SignInEvent
import com.a32b.plant.presentation.auth.viewmodel.SignInViewModel
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.theme.LocalIsDarkTheme
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(navController: NavController, viewModel: SignInViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current

    val webClientId = stringResource(R.string.default_web_client_id)
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.ShowToast ->
                    context.showToast(
                        event.message,
                        if (event.isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                    )

                is SignInEvent.NavigateToHome ->
                    navController.navigate(Routes.HomeMain) {
                        popUpTo(0) { inclusive = true }
                    }
            }
        }
    }

    // 닉네임 설정 다이얼로그
    if (uiState.showNicknameDialog) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(if (isDark) 0.dp else 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "닉네임 설정",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Plant에서 사용 할 닉네임을 설정해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    TextField(
                        value = uiState.nicknameInput,
                        onValueChange = viewModel::onNicknameChange,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholder = {
                            Text(
                                text = "2~10자 닉네임 입력",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedPlaceholderColor = MaterialTheme.colorScheme.onSecondary,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = uiState.nicknameError != null,
                        supportingText = uiState.nicknameError?.let {
                            {
                                Text(
                                    text = it,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = viewModel::confirmNickname,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !uiState.isNicknameLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = if (isDark) 0.dp else 4.dp,
                            pressedElevation = if (isDark) 0.dp else 2.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (uiState.isNicknameLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "설정 완료",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.background,
                            )
                        }
                    }
                }
            }
        }
    }

    // 구글 계정 선택 → 토큰 획득 → ViewModel 전달 (로그인 로직은 기존과 동일)
    val startGoogleSignIn: () -> Unit = startGoogleSignIn@{
        if (!viewModel.tryStartGoogleSignIn()) return@startGoogleSignIn

        coroutineScope.launch {
            val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context as Activity
                )

                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.handleGoogleSignIn(googleIdTokenCredential.idToken)

            } catch (e: GetCredentialCancellationException) {
                // 사용자가 계정 선택창을 닫은 경우 무시
                viewModel.cancelGoogleSignIn()
            } catch (e: NoCredentialException) {
                viewModel.cancelGoogleSignIn()
                context.showToast("기기에 등록된 Google 계정이 없습니다. 설정에서 계정을 추가해주세요.")
                val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                intent.putExtra("account_types", arrayOf("com.google"))
                context.startActivity(intent)
            } catch (e: CancellationException) {
                // 화면 이탈 등으로 코루틴이 취소된 경우: 가드만 풀어주고 취소는 그대로 전파
                viewModel.cancelGoogleSignIn()
                throw e
            } catch (e: Exception) {
                viewModel.cancelGoogleSignIn()
                Log.e("SignIn", "구글 로그인 에러: ${e.message}", e)
                context.showToast("구글 로그인에 실패했습니다.\n다시 시도해주세요.")
            }
        }
    }

    LoadableScreen(viewModel) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            // 화면이 충분히 크면 카드를 하단에 붙이고, 작은 화면/큰 글씨에서는 스크롤
            // (상태바·내비바 여백은 MainActivity Scaffold가 이미 적용·소비하므로 여기서 따로 주지 않음)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // 상단 브랜드 영역
                BrandSection(modifier = Modifier.padding(vertical = 32.dp))

                Spacer(modifier = Modifier.weight(1f))

                // 하단 로그인 카드
                SignInCard(
                    isLoading = uiState.isLoading,
                    onGoogleSignInClick = startGoogleSignIn
                )
            }
        }
    }
}

// 화분 성장 단계 이미지 (lv0 → lv6)
// 일반 화분 이미지는 가운데 정렬이라 레벨마다 화분 위치가 달라짐 → 애니메이션용은 화분 위치를 고정한 별도 리소스 사용
private val potGrowthImages = listOf(
    R.drawable.ic_pot_growth_lv0,
    R.drawable.ic_pot_growth_lv1,
    R.drawable.ic_pot_growth_lv2,
    R.drawable.ic_pot_growth_lv3,
    R.drawable.ic_pot_growth_lv4,
    R.drawable.ic_pot_growth_lv5,
    R.drawable.ic_pot_growth_lv6
)

@Composable
private fun BrandSection(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PotGrowthAnimation(modifier = Modifier.size(130.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Plant",
            style = MaterialTheme.typography.headlineLarge // primary 위 글자색(onPrimary)이 스타일에 지정되어 있음
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "공부할수록 자라는 나만의 화분",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 화분이 lv0 → lv6 으로 자라는 모습을 반복 재생
 * - 단계마다 일정 시간 머무르고, 마지막 단계(lv6)는 조금 더 오래 보여준 뒤 처음으로 돌아감
 */
@Composable
private fun PotGrowthAnimation(modifier: Modifier = Modifier) {
    var stepIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            val isLastStep = stepIndex == potGrowthImages.lastIndex
            delay(if (isLastStep) POT_LAST_STEP_DURATION_MS else POT_STEP_DURATION_MS)
            stepIndex = (stepIndex + 1) % potGrowthImages.size
        }
    }

    Crossfade(
        targetState = potGrowthImages[stepIndex],
        modifier = modifier,
        animationSpec = tween(durationMillis = POT_CROSSFADE_DURATION_MS),
        label = "PotGrowthAnimation"
    ) { resId ->
        Image(
            painter = painterResource(id = resId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun SignInCard(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "간편하게 시작해 보세요",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp) // fillMaxWidth보다 먼저 와야 최대 너비가 적용됨
                    .fillMaxWidth()
                    .clip(GoogleSignInButtonShape) // 로딩 오버레이가 버튼 모양 밖으로 나가지 않도록 버튼과 같은 모양 사용
            ) {
                GoogleSignInButton(
                    onClick = onGoogleSignInClick,
                    modifier = Modifier.fillMaxWidth()
                )

                // 로그인 진행 중: 버튼 위 로딩 + 터치 차단 (중복 탭은 tryStartGoogleSignIn 가드로도 방지)
                if (isLoading) {
                    LoadingBox(modifier = Modifier.matchParentSize())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "로그인 시 서비스 이용약관 및 개인정보처리방침에\n동의하는 것으로 간주됩니다.",
                // 가장 작은 타이포(12sp)보다 작게 표시하기 위해 labelMedium(힌트용)을 축소해서 사용
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 16.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val POT_STEP_DURATION_MS = 700L
private const val POT_LAST_STEP_DURATION_MS = 1600L
private const val POT_CROSSFADE_DURATION_MS = 400
