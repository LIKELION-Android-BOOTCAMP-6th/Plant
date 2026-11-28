package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.a32b.plant.R
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.core.extension.resId
import com.a32b.plant.presentation.home.viewmodel.LevelUpDialogUiState
import com.a32b.plant.presentation.home.viewmodel.LevelUpItemRequirement
import com.a32b.plant.presentation.theme.LocalIsDarkTheme
import com.a32b.plant.presentation.theme.PlantTheme
import com.a32b.plant.presentation.theme.deleteAccountButtonContentDark

/**
 * 레벨업 확인 다이얼로그 (#678)
 * 필요한 재료(보유/필요)와 학습 시간(현재/필요)을 보여주고, "레벨업"을 누르면 onConfirm
 * 조건이 부족하면 버튼을 alpha로만 흐리게 한다 — 누르면 ViewModel이 부족한 내용을 토스트로 안내 (비활성 버튼 + Toast 패턴)
 * isLoading: 레벨업 처리 중 — 버튼·취소·바깥 터치를 막는다
 */
@Composable
fun LevelUpDialog(
    state: LevelUpDialogUiState,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        // 다이얼로그 배경은 ConfirmDialog와 같은 primaryContainer로 통일 (#643)
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        title = {
            Text(
                text = "Lv.${state.currentLevel} → Lv.${state.newLevel} 레벨업",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = { LevelUpDialogContent(state) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .alpha(if (state.isReady) 1f else 0.38f)
                    // alpha는 시각 표시뿐이라 TalkBack에 상태를 따로 알림 (홈 레벨업 버튼과 같은 문구)
                    .semantics { if (!state.isReady) stateDescription = LEVEL_UP_NOT_READY_STATE_DESCRIPTION },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isLoading) "레벨업 중..." else "레벨업", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.common_cancel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * 다이얼로그 본문: 필요한 재료 목록 + 학습 시간
 * Dialog 안에는 테스트의 글꼴 배율 덮어쓰기(LocalDensity)가 닿지 않아, 큰 글꼴 확인용으로 본문을 따로 둔다 (LevelUpDialogTest.contentTextsStayOnOneLineWithoutOverlapInLargeFont)
 */
@Composable
internal fun LevelUpDialogContent(state: LevelUpDialogUiState) {
    // 큰 글꼴에서 재료가 많으면(최대 5종) 다이얼로그 높이를 넘을 수 있어 본문만 스크롤되게 한다
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "필요한 재료 (보유 / 필요)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondary
        )
        state.items.forEach { requirement ->
            LevelUpItemRow(requirement)
        }

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = "학습 시간 (현재 / 필요)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondary
        )
        Text(
            text = "${state.currentStudyTimeText} / ${state.requiredStudyTimeText}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.isStudyTimeEnough) MaterialTheme.colorScheme.onSurface else dialogErrorTextColor()
        )
    }
}

/**
 * 재료 한 줄: 아이콘 + 이름 + "보유 / 필요개" — 부족하면 개수를 붉은색으로 강조한다
 * 얼마나 부족한지는 "보유 / 필요개" 숫자로 알 수 있어 따로 적지 않는다 (#805). 숫자가 글자로 남아 있어 색만으로 알리는 것은 아니다
 */
@Composable
private fun LevelUpItemRow(requirement: LevelUpItemRequirement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 아이콘·이름·개수를 TalkBack이 한 번에 읽도록 묶음
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = requirement.type.resId),
            // 이름을 옆에 글자로 쓰므로 아이콘은 읽지 않음
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = requirement.type.kor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${requirement.owned} / ${requirement.required}개",
            style = MaterialTheme.typography.titleSmall,
            color = if (requirement.isEnough) MaterialTheme.colorScheme.tertiary else dialogErrorTextColor()
        )
    }
}

// 다크 다이얼로그 배경(primaryContainer 414141) 위 error(CF6262)는 대비 약 2.71:1이라
// 복구 다이얼로그와 같은 다크 전용 글자색(EC928E, 약 4.41:1)을 쓴다 (#643)
@Composable
private fun dialogErrorTextColor(): Color =
    if (LocalIsDarkTheme.current) deleteAccountButtonContentDark else MaterialTheme.colorScheme.error

@Preview(showBackground = true)
@Composable
private fun LevelUpDialogPreview() {
    PlantTheme {
        LevelUpDialog(
            state = LevelUpDialogUiState(
                potId = "pot",
                currentLevel = 1,
                items = listOf(
                    LevelUpItemRequirement(ItemType.HEART, owned = 6, required = 5),
                    LevelUpItemRequirement(ItemType.SUN, owned = 1, required = 3),
                    LevelUpItemRequirement(ItemType.WATER, owned = 0, required = 1)
                ),
                currentStudyTimeText = "3시간 20분",
                requiredStudyTimeText = "5시간",
                remainingStudyTimeText = "1시간 40분"
            ),
            isLoading = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
