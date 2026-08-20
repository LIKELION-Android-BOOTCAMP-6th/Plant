package com.a32b.plant.presentation.mypage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.mypage.viewmodel.MyPageEvent
import com.a32b.plant.presentation.mypage.viewmodel.MyPageSettingViewModel

@Composable
fun MyPageSettingScreen(
    navController: NavController,
    viewModel: MyPageSettingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 회원탈퇴 2단계 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteSecondConfirm by remember { mutableStateOf(false) }

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
                    viewModel.deleteAccount()
                } else {
                    isDeleteSecondConfirm = true
                }
            }
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
