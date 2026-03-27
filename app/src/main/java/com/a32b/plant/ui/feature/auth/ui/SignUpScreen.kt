package com.a32b.plant.ui.feature.auth.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.auth.viewmodel.SignUpEvent
import com.a32b.plant.ui.feature.auth.viewmodel.SignUpViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.fontColorSub
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.textFieldBackground

@Composable
fun SignUpScreen(
    navController: NavController
) {
    val viewModel: SignUpViewModel = viewModel(factory = ViewModelFactory.signUpViewModelFactory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }
    var passwordConfirmVisible by remember { mutableStateOf(false) }

    // 토스트 & 화면 전환 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SignUpEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                is SignUpEvent.NavigateToSignIn ->
                    navController.navigate(Routes.SignIn) {
                        popUpTo(Routes.SignUp) { inclusive = true }
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
            Spacer(modifier = Modifier.height(130.dp))

            // Plant 타이틀 — 카드 위 초록 배경에 흰색으로
            Text(
                text = "Plant",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = background,
                fontSize = 40.sp
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

                    // 이메일
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
                                text = "이메일을 입력하세요.",
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

                    // 비밀번호
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
                                text = "6자리 이상. 영문,특수문자,숫자 필수.",
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
                        shape = RoundedCornerShape(8.dp),
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 비밀번호 확인
                    Text(
                        text = "비밀번호 확인",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = uiState.passwordConfirm,
                        onValueChange = viewModel::onPasswordConfirmChange,
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
                        visualTransformation = if (passwordConfirmVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                passwordConfirmVisible = !passwordConfirmVisible
                            }) {
                                Icon(
                                    imageVector = if (passwordConfirmVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = uiState.passwordConfirmError != null,
                        supportingText = uiState.passwordConfirmError?.let { { Text(it) } }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // 회원가입 완료 버튼
                    Button(
                        onClick = viewModel::signUp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        // 그림자 효과
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,    // 기본 상태 그림자 높이
                            pressedElevation = 2.dp,    // 버튼 누를 때 그림자 높이 (누르면 낮아지는 효과)
                            disabledElevation = 0.dp    // 비활성화 상태일 때 그림자 높이 0
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
                                "회원가입 완료",
                                style = MaterialTheme.typography.bodyMedium,
                                color = background,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row {
                            Text(
                                text = "이미 계정이 있으신가요? ",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                color = fontColor
                            )
                            Text(
                                text = "로그인",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier.clickable { navController.popBackStack() }
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
