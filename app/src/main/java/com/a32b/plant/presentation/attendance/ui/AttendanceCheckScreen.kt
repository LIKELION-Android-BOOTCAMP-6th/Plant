package com.a32b.plant.presentation.attendance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AttendanceCheckScreen(navController: NavController) {
    AttendanceCheckContent(
        checkedCount = 8,
        rewards = sampleRewards
    )
}

@Composable
private fun AttendanceCheckContent(
    checkedCount: Int,
    rewards: List<AttendanceRewardUi>
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
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
                checkedCount = checkedCount,
                rewards = rewards
            )

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "출석하기",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
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
            .padding(top = 28.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column {
            Text(
                text = "이번 달 출석판",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${checkedCount}/${rewards.size}일 출석",
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
            trackColor = MaterialTheme.colorScheme.tertiary
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
        isNext -> MaterialTheme.colorScheme.secondaryContainer
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
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isChecked) "✓" else reward.day.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isChecked || isNext) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
        }

        Text(
            text = reward.label.ifBlank { " " },
            modifier = Modifier.width(42.dp),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendanceCheckContentPreview() {
    AttendanceCheckContent(
        checkedCount = 8,
        rewards = sampleRewards
    )
}


private data class AttendanceRewardUi(
    val day: Int,
    val label: String = ""
)

private val sampleRewards = listOf(
    AttendanceRewardUi(1),
    AttendanceRewardUi(2, "하트"),
    AttendanceRewardUi(3),
    AttendanceRewardUi(4, "햇빛"),
    AttendanceRewardUi(5),
    AttendanceRewardUi(6),
    AttendanceRewardUi(7, "100G"),
    AttendanceRewardUi(8),
    AttendanceRewardUi(9, "물"),
    AttendanceRewardUi(10),
    AttendanceRewardUi(11),
    AttendanceRewardUi(12, "하트"),
    AttendanceRewardUi(13),
    AttendanceRewardUi(14, "200G"),
    AttendanceRewardUi(15),
    AttendanceRewardUi(16),
    AttendanceRewardUi(17, "햇빛"),
    AttendanceRewardUi(18),
    AttendanceRewardUi(19),
    AttendanceRewardUi(20, "비료"),
    AttendanceRewardUi(21, "300G"),
    AttendanceRewardUi(22),
    AttendanceRewardUi(23, "물"),
    AttendanceRewardUi(24),
    AttendanceRewardUi(25, "영양제"),
    AttendanceRewardUi(26),
    AttendanceRewardUi(27),
    AttendanceRewardUi(28, "500G")
)


