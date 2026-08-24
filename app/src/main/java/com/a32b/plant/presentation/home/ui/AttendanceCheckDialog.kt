package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.a32b.plant.R
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.AttendanceRewardTable
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.core.extension.resId
import com.a32b.plant.presentation.home.viewmodel.AttendanceUiState
import com.a32b.plant.presentation.theme.PlantTheme

@Composable
fun AttendanceCheckDialog(
    uiState: AttendanceUiState,
    onCheckClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AttendanceDialogContent(uiState, onCheckClick)
    }
}

// Dialog는 @Preview에서 제대로 그려지지 않을 수 있어 내용물을 분리해 미리보기에 쓴다.
@Composable
private fun AttendanceDialogContent(
    uiState: AttendanceUiState = AttendanceUiState(),
    onCheckClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "출석체크",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "출석할수록 보상이 자라나요",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AttendanceBoard(
                checkedCount = uiState.count,
                rewards = attendanceRewards
            )

            Button(
                onClick = onCheckClick,
                enabled = uiState.buttonEnabled && !uiState.isChecking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text(uiState.buttonText)
            }
        }
    }
}

@Preview(showBackground = true, name = "출석체크 다이얼로그", widthDp = 360)
@Preview(showBackground = true, name = "출석체크 다이얼로그 - 폴드3 접음", widthDp = 350)
@Preview(showBackground = true, name = "출석체크 다이얼로그 - 폴드3 펼침", widthDp = 673)
@Composable
private fun AttendanceDialogContentPreview() {
    PlantTheme {
        AttendanceDialogContent(
            uiState = AttendanceUiState(
                count = 8,
                buttonText = "출석하기",
                buttonEnabled = true
            )
        )
    }
}

@Composable
private fun AttendanceBoard(
    checkedCount: Int,
    rewards: List<AttendanceRewardUi>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "이번 달 출석판",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${rewards.size}칸 중 ${checkedCount}칸 완료",
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { checkedCount.toFloat() / rewards.size },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.secondary,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rewards.chunked(7).forEach { rowRewards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowRewards.forEach { reward ->
                        AttendanceRewardItem(
                            reward = reward,
                            isChecked = reward.day <= checkedCount,
                            isNext = reward.day == checkedCount + 1
                        )
                    }
                }
            }
        }

        Text(
            text = "다음 출석 보상: ${rewards.getOrNull(checkedCount)?.label?.ifBlank { "기본 보상" } ?: "기본 보상"}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AttendanceRewardItem(
    reward: AttendanceRewardUi,
    isChecked: Boolean,
    isNext: Boolean
) {
    val backgroundColor = when {
        isChecked -> MaterialTheme.colorScheme.primary
        isNext -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.background
    }

    val borderColor = when {
        isChecked -> MaterialTheme.colorScheme.primary
        isNext -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }

    val textColor = when {
        isChecked -> MaterialTheme.colorScheme.onPrimary
        isNext -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(if (isNext) 2.dp else 1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isChecked -> Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )

                reward.iconRes != null -> Image(
                    painter = painterResource(id = reward.iconRes),
                    contentDescription = reward.label.ifBlank { "보상" },
                    modifier = Modifier.size(22.dp)
                )

                else -> Text(
                    text = reward.day.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }

        Text(
            text = if (reward.showLabelBelow) reward.label else " ",
            modifier = Modifier.width(42.dp),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * label   보상 이름. "다음 출석 보상" 안내 문구에 쓰이므로 아이콘이 있어도 비우지 않는다.
 * iconRes 원 안에 표시할 아이콘.
 * showLabelBelow 원 아래에도 글씨를 보일지. 금액 구분이 필요한 골드만 true.
 */
private data class AttendanceRewardUi(
    val day: Int,
    val label: String = "",
    val iconRes: Int? = null,
    val showLabelBelow: Boolean = false
)

private fun ItemType.attendanceLabel(): String = when (this) {
    ItemType.HEART -> "하트"
    ItemType.SUN -> "햇빛"
    ItemType.WATER -> "물"
    ItemType.FERTILIZER -> "비료"
    ItemType.NUTRIENT -> "영양제"
    ItemType.BOX -> "박스"
    ItemType.GOLD_300, ItemType.GOLD_500, ItemType.GOLD_1000 ->
        error("출석 보상 테이블에 골드 아이템 타입은 나오지 않습니다: $this")
}

private val attendanceRewards: List<AttendanceRewardUi> = (1..28).map { day ->
    when (val reward = AttendanceRewardTable.of(day)) {
        is AttendanceReward.Coin ->
            AttendanceRewardUi(day, "${reward.amount}G", R.drawable.ic_coin, showLabelBelow = true)

        is AttendanceReward.ItemReward ->
            AttendanceRewardUi(day, reward.type.attendanceLabel(), reward.type.resId)

        null -> AttendanceRewardUi(day)
    }
}
