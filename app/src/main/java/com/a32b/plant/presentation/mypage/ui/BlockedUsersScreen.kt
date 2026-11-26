package com.a32b.plant.presentation.mypage.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.rememberPopBackStackSafely
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.presentation.core.component.BackTopBar
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.mypage.viewmodel.BlockedUsersEvent
import com.a32b.plant.presentation.mypage.viewmodel.BlockedUsersViewModel

/**
 * 마이페이지 > 차단 목록 (#660)
 * 닉네임·프로필은 차단한 시점의 값이다 (상대가 닉네임을 바꿔도 갱신하지 않음)
 */
@Composable
fun BlockedUsersScreen(
    navController: NavController,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val popBackStackSafely = rememberPopBackStackSafely(navController)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is BlockedUsersEvent.ShowToast -> context.showToast(event.message)
            }
        }
    }

    uiState.unblockTarget?.let { target ->
        ConfirmDialog(
            text = "${target.nickname}님의 차단을 해제할까요?",
            semiText = "해제하면 글과 댓글이 다시 보여요.",
            confirmBtnText = "해제",
            isLoading = uiState.isLoading,
            onDismiss = { viewModel.closeUnblockDialog() },
            onConfirm = { viewModel.confirmUnblock() }
        )
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "차단 목록",
                onBack = { popBackStackSafely() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LoadableScreen(viewModel) {
                if (uiState.users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "차단한 사용자가 없어요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.users, key = { it.uid }) { user ->
                            BlockedUserRow(
                                user = user,
                                onUnblockClick = { viewModel.openUnblockDialog(user) }
                            )
                        }
                    }
                }
            }

            // 해제 처리 중 — 해제하면 목록에서 바로 빠져 확인 창이 먼저 닫히므로, 처리가 끝날 때까지(오프라인이면 최대 5초) 여기서 표시한다
            if (uiState.isLoading) LoadingBox()
        }
    }
}

@Composable
private fun BlockedUserRow(
    user: BlockedUser,
    onUnblockClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileImage(user.profileImg, 36)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = user.nickname,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onUnblockClick) {
            Text(
                text = "차단 해제",
                style = MaterialTheme.typography.labelLarge,
                // labelLarge 기본색(onSurface) 대신 누를 수 있는 동작임을 드러내는 색
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
