package com.a32b.plant.ui.feature.mypage.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageEvent
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageSettingViewModel
import com.a32b.plant.ui.theme.Typography

import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import com.a32b.plant.ui.theme.background
// 다이얼로그 상태용
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import com.a32b.plant.ui.theme.sub2
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.ui.theme.PlantTheme

@Composable
fun MyPageSettingScreen(navController: NavController) {
    val viewModel: MyPageSettingViewModel =
        viewModel(factory = ViewModelFactory.myPageSettingViewModelFactory)
    val context = LocalContext.current

    // 회원탈퇴 2단계 확인 다이얼로그 상태
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleteSecondConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MyPageEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                is MyPageEvent.NavigateToSignIn ->
                    navController.navigate(Routes.SignIn) {
                        // 모든 백스택 제거 → 뒤로가기해도 홈으로 안 돌아가게
                        popUpTo(0) { inclusive = true }
                    }

                is MyPageEvent.NavigateToMyCommunityFeed ->
                    navController.navigate(Routes.MyCommunityFeed)
            }
        }
    }

// 회원탈퇴 2단계 확인 다이얼로그
    // 1단계: "탈퇴 하시겠습니까?" → 확인 클릭 시 2단계로 전환 (다이얼로그 유지, 문구만 변경)
    // 2단계: "정말로 탈퇴하시겠습니까?" → 확인 클릭 시 실제 탈퇴 실행
    if (showDeleteDialog) {
        ConfirmDialog(
            text = if (isDeleteSecondConfirm) "정말로 탈퇴하시겠습니까?"
            else "탈퇴 하시겠습니까?",
            semiText = if (isDeleteSecondConfirm) "탈퇴 시 모든 학습 기록이 삭제되며 복구할 수 없습니다."
            else "계정을 삭제하시려면 '예'를 눌러주세요.",
            onDismiss = {
                showDeleteDialog = false
                isDeleteSecondConfirm = false   // 닫으면 1단계로 리셋
            },
            onConfirm = {
                if (isDeleteSecondConfirm) {
                    // 2단계 확인 → 실제 탈퇴 실행
                    showDeleteDialog = false
                    isDeleteSecondConfirm = false
                    viewModel.deleteAccount()
                } else {
                    // 1단계 확인 → 2단계로 전환
                    isDeleteSecondConfirm = true
                }
            }
        )
    }
    // *********************************
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
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
//        ButtonTemplate(text = "이용약관") { }
//        ButtonTemplate(text = "FAQ") { }
//        ButtonTemplate(text = "사용설명서") { }
//        ButtonTemplate(text = "앱 테마") { }

            // **************************수정: 직접 deleteAccount() 호출 대신 다이얼로그 띄우기
            ButtonTemplate(text = "회원탈퇴") {
                isDeleteSecondConfirm = false    // 열 때마다 1단계부터
                showDeleteDialog = true
            }
            // **************************
//------------------회원탈퇴
        }
    }
}

