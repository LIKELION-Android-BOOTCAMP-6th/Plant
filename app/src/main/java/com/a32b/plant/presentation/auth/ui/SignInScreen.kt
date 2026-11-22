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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.a32b.plant.domain.util.NicknameRule
import com.a32b.plant.presentation.auth.viewmodel.SignInEvent
import com.a32b.plant.presentation.auth.viewmodel.SignInViewModel
import com.a32b.plant.presentation.auth.viewmodel.SignUpConsent
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.plantCheckboxColors
import com.a32b.plant.presentation.core.extension.openUrl
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

                is SignInEvent.OpenUrl -> context.openUrl(event.url)

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
                        // 동의 항목이 추가돼 작은 화면·큰 글씨에서 다이얼로그가 화면을 넘을 수 있어 스크롤
                        .verticalScroll(rememberScrollState())
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
                                text = "${NicknameRule.MIN_LENGTH}~${NicknameRule.MAX_LENGTH}자 닉네임 입력",
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 필수 동의 (#661)
                    ConsentSection(
                        agreedConsents = uiState.agreedConsents,
                        isAllConsented = uiState.isAllConsented,
                        enabled = !uiState.isNicknameLoading,
                        onAllConsentChange = viewModel::onAllConsentChange,
                        onConsentChange = viewModel::onConsentChange,
                        onTermsOfServiceClick = viewModel::onTermsOfServiceClick,
                        onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 동의가 덜 됐으면 흐리게만 표시하고 누를 수는 있게 둔다 (누르면 ViewModel이 토스트로 안내)
                    // enabled = false로 막으면 onClick이 호출되지 않아 토스트를 띄울 수 없다
                    Button(
                        onClick = viewModel::confirmNickname,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .alpha(if (uiState.isAllConsented) 1f else DISABLED_LOOK_ALPHA),
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
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 동의하지 않고 나가는 경로 (만 14세 미만 등). 다음 로그인 때 다시 닉네임 설정·동의를 받는다
                    TextButton(
                        onClick = viewModel::signOutFromNicknameDialog,
                        enabled = !uiState.isNicknameLoading
                    ) {
                        Text(
                            text = "로그아웃",
                            style = MaterialTheme.typography.bodySmall
                        )
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
        // 화분 그림이 이미지 폭의 약 41%라서(Lv6까지 담기 위해 축소) 영역을 키워 화분이 작아 보이지 않게 함
        PotGrowthAnimation(modifier = Modifier.size(170.dp))

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
            // 약관·개인정보처리방침 동의는 첫 로그인 닉네임 설정 다이얼로그에서 직접 체크받는다 (#661)
        }
    }
}

/**
 * 첫 로그인 필수 동의 영역 (#661)
 * "전체 동의"는 세 항목을 함께 체크/해제하고, 세 항목이 모두 체크되면 함께 체크 상태로 보인다 ([isAllConsented])
 */
@Composable
private fun ConsentSection(
    agreedConsents: Set<SignUpConsent>,
    isAllConsented: Boolean,
    enabled: Boolean,
    onAllConsentChange: (Boolean) -> Unit,
    onConsentChange: (SignUpConsent, Boolean) -> Unit,
    onTermsOfServiceClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ConsentRow(
            text = "전체 동의",
            checked = isAllConsented,
            enabled = enabled,
            onCheckedChange = onAllConsentChange,
            textStyle = MaterialTheme.typography.titleSmall
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.outline
        )

        ConsentRow(
            text = "[필수] 만 14세 이상이에요",
            checked = SignUpConsent.AGE_14_OR_OVER in agreedConsents,
            enabled = enabled,
            onCheckedChange = { onConsentChange(SignUpConsent.AGE_14_OR_OVER, it) }
        )
        ConsentRow(
            text = "[필수] 서비스 이용약관에 동의해요",
            checked = SignUpConsent.TERMS_OF_SERVICE in agreedConsents,
            enabled = enabled,
            onCheckedChange = { onConsentChange(SignUpConsent.TERMS_OF_SERVICE, it) },
            onViewClick = onTermsOfServiceClick
        )
        ConsentRow(
            text = "[필수] 개인정보 처리방침에 동의해요",
            checked = SignUpConsent.PRIVACY_POLICY in agreedConsents,
            enabled = enabled,
            onCheckedChange = { onConsentChange(SignUpConsent.PRIVACY_POLICY, it) },
            onViewClick = onPrivacyPolicyClick
        )
    }
}

/** 체크박스 + 문구 한 줄. 문구를 눌러도 체크가 바뀌고, [onViewClick]이 있으면 오른쪽에 "보기"를 둔다 */
@Composable
private fun ConsentRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    onViewClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 줄 전체(toggleable)가 체크를 처리하므로 체크박스 자체는 클릭을 받지 않는다
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = plantCheckboxColors(),
                modifier = Modifier.minimumInteractiveComponentSize()
            )
            // 세로 여백: 한 줄이면 체크박스 높이(48dp) 안에 들어가 그대로이고, 두 줄로 넘어가면 위아래 항목과 붙지 않게 한다
            // WordBreak.Phrase: 단어 중간에서 줄이 바뀌지 않게 띄어쓰기 단위로 바꾼다 (지원하지 않는 Android 버전에서는 글자 단위)
            // 적용 여부가 문구 언어(지정하지 않으면 기기 언어)에 따라 달라, 기기 언어와 상관없이 적용되게 ko-KR로 지정한다
            // (기기 언어가 영어인 에뮬레이터에서 지정하기 전에는 글자 단위로 바뀌었음)
            Text(
                text = text,
                style = textStyle.copy(
                    lineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase),
                    localeList = LocaleList("ko-KR")
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        if (onViewClick != null) {
            TextButton(onClick = onViewClick) {
                Text(
                    text = "보기",
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

// 필수 동의가 덜 된 확인 버튼을 흐리게 보이는 정도 (누를 수는 있음)
private const val DISABLED_LOOK_ALPHA = 0.4f

private const val POT_STEP_DURATION_MS = 700L
private const val POT_LAST_STEP_DURATION_MS = 1600L
private const val POT_CROSSFADE_DURATION_MS = 400
