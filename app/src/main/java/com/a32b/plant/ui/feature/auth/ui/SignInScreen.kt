package com.a32b.plant.ui.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.auth.viewmodel.SignInEvent
import com.a32b.plant.ui.feature.auth.viewmodel.SignInViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.textFieldBackground

@Composable
fun SignInScreen(navController: NavController) {
    val viewModel: SignInViewModel = viewModel(factory = ViewModelFactory.signInViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }

    // 일회성 이벤트 수신 (토스트, 화면 전환)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignInEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                is SignInEvent.NavigateToHome ->
                    navController.navigate(Routes.HomeMain) {
                        // 로그인 화면을 백스택에서 제거 → 뒤로가기 시 로그인으로 돌아가지 않도록
                        popUpTo(Routes.SignIn) { inclusive = true }
                    }

                is SignInEvent.NavigateToSignUp ->
                    navController.navigate(Routes.SignUp)
            }
        }
    }

    // *****************************************************************************************
    // 닉네임 설정 다이얼로그 (isFirstLogin == true 일 때 표시) - StudyingScreen Dialog와 비슷하게 맞추기!
    if (uiState.showNicknameDialog) {
        Dialog(
            onDismissRequest = {}, // 닫기 불가 - 닉네임 설정 필수
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
                colors = CardDefaults.cardColors(containerColor = background),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "닉네임 설정",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Plant에서 사용 할 닉네임을 설정해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = fontColorSub
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 입력창
                    TextField(
                        value = uiState.nicknameInput,
                        onValueChange = viewModel::onNicknameChange,
                        placeholder = {
                            Text(
                                text = "2~10자 닉네임 입력",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                        shape = RoundedCornerShape(8.dp),
                        isError = uiState.nicknameError != null,
                        supportingText = uiState.nicknameError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 설정 완료 버튼
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
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp,
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = background,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // 초록 배경 전체
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(100.dp))

            // Plant 로고 타이틀
            Text(
                text = "Plant",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = background,
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            //  서브 타이틀
            Text(
                text = "로그인해서 계속하세요",
                style = MaterialTheme.typography.bodyMedium,
                color = background,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 흰색 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = background),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // 이메일 입력
                    Text(
                        text = "이메일",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        placeholder = {
                            Text(
                                text = "이메일을 입력하세요",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 입력
                    Text(
                        text = "비밀번호",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        placeholder = {
                            Text(
                                text = "비밀번호를 입력하세요",
                                style = MaterialTheme.typography.bodySmall
                            )
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
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 비밀번호 찾기 링크 (우측 정렬)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "비밀번호를 잊으셨나요?",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = fontColorSub,
                            modifier = Modifier.clickable {
                                // TODO: FindPasswordDialog 연결
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 로그인 버튼
                    Button(
                        onClick = viewModel::signIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "로그인",
                                style = MaterialTheme.typography.bodyMedium,
                                color = background,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── "또는" 구분선 ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_signin_divider_2),
                            contentDescription = null,
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.FillWidth
                        )
                        Text(
                            text = "또는",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = fontColorSub,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_signin_divider_2),
                            contentDescription = null,
                            modifier = Modifier.weight(1f),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 구글 로그인 버튼
                    OutlinedButton(
                        onClick = {
                            // TODO: 구글 로그인 (후순위 개발)
                            Toast.makeText(context, "구글 로그인은 추후 지원 예정입니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_auth_google),
                            contentDescription = "Google 로그인",
                            modifier = Modifier.height(40.dp),
                            contentScale = ContentScale.FillHeight
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "계정이 없으신가요? 회원가입" 링크
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row {
                            Text(
                                text = "계정이 없으신가요? ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                color = fontColor
                            )
                            Text(
                                text = "회원가입",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    navController.navigate(Routes.SignUp)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}