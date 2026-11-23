package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.a32b.plant.R
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.AttendanceRewardTable
import com.a32b.plant.presentation.core.component.LoadingBox
import com.a32b.plant.presentation.core.extension.resId
import com.a32b.plant.presentation.home.viewmodel.AttendanceUiState
import com.a32b.plant.presentation.theme.PlantTheme

@Composable
fun AttendanceCheckDialog(
    uiState: AttendanceUiState,
    isOnline: Boolean,
    onCheckClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        // 출석 처리 중에는 바깥 터치·뒤로가기로 닫히지 않게 한다.
        onDismissRequest = { if (!uiState.isChecking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AttendanceDialogContent(uiState, isOnline, onCheckClick, onDismiss)
    }
}

// Dialog는 @Preview에서 제대로 그려지지 않을 수 있어 내용물을 분리해 미리보기에 쓴다.
// 계측 테스트도 이 함수를 그린다 (Dialog 안에는 테스트에서 바꾼 글꼴 배율이 닿지 않음)
@Composable
internal fun AttendanceDialogContent(
    uiState: AttendanceUiState = AttendanceUiState(),
    isOnline: Boolean = true,
    onCheckClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // weight(fill = false): 화면이 충분하면 내용 높이만큼만, 짧으면 버튼 두 개를 놓고 남은 높이까지만 차지해서
                // 제목·출석판만 스크롤되고 출석하기·나가기 버튼은 항상 보이게 한다
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "출석체크",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "출석할수록 보상이 자라나요",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondary
                    )

                    AttendanceBoard(
                        checkedCount = uiState.count,
                        rewards = attendanceRewards
                    )
                }

                // 오프라인: 출석은 온라인 전용이라 비활성처럼 보이게 한다 (#531)
                // enabled = false면 클릭 자체가 막혀 안내 토스트를 띄울 수 없으므로 alpha로 시각만 처리하고,
                // 누르면 ViewModel이 요청 없이 안내한다
                val isOfflineLooking = !isOnline && !uiState.isAttendanceCompleted
                Button(
                    onClick = onCheckClick,
                    enabled = !uiState.isAttendanceCompleted && !uiState.isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                        .alpha(if (isOfflineLooking) OFFLINE_BUTTON_ALPHA else 1f)
                        // alpha는 시각 표시뿐이라 TalkBack에 상태를 따로 알림
                        .semantics { if (isOfflineLooking) stateDescription = OFFLINE_STATE_DESCRIPTION }
                ) {
                    Text(uiState.buttonText, style = MaterialTheme.typography.bodyMedium)
                }

                TextButton(
                    onClick = onDismiss,
                    enabled = !uiState.isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("나가기", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (uiState.isChecking) {
                LoadingBox(
                    modifier = Modifier.matchParentSize()
                )
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
                buttonText = "출석하기"
            )
        )
    }
}

@Preview(showBackground = true, name = "출석체크 다이얼로그 - 오프라인", widthDp = 360)
@Composable
private fun AttendanceDialogContentOfflinePreview() {
    PlantTheme {
        AttendanceDialogContent(
            uiState = AttendanceUiState(
                count = 8,
                buttonText = "출석하기"
            ),
            isOnline = false
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
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
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
                color = MaterialTheme.colorScheme.onSecondary
            )
        }

        LinearProgressIndicator(
            progress = { checkedCount.toFloat() / rewards.size },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                            isNext = reward.day == checkedCount + 1,
                            // weight(fill = false): 폭이 넉넉하면 지금처럼 SpaceBetween으로 벌어지고,
                            // 좁으면(360dp 등) 7칸이 같은 폭으로 줄어 마지막 칸이 잘리지 않게 한다
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        Text(
            text = "다음 출석 보상: ${rewards.getOrNull(checkedCount)?.label?.ifBlank { "기본 보상" } ?: "기본 보상"}",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AttendanceRewardItem(
    reward: AttendanceRewardUi,
    isChecked: Boolean,
    isNext: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isChecked -> MaterialTheme.colorScheme.primary
        isNext -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.background
    }

    val borderColor = when {
        isChecked -> MaterialTheme.colorScheme.primary
        isNext -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val textColor = when {
        isChecked -> MaterialTheme.colorScheme.onPrimary
        isNext -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSecondary
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            // 칸이 42dp보다 좁으면 양옆 2dp 여백을 남기고 줄어들며, 원 모양은 유지한다
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .widthIn(max = 38.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(if (isNext) 2.dp else 1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isChecked -> Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor
                )

                reward.iconRes != null -> Image(
                    painter = painterResource(id = reward.iconRes),
                    contentDescription = reward.label.ifBlank { "보상" },
                    modifier = Modifier.size(22.dp)
                )

                else -> Text(
                    text = reward.day.toString(),
                    // 다음 출석 칸만 굵게 — 같은 12sp에서 글꼴만 다르게 구분 (titleSmall은 R.font.medium, bodySmall은 R.font.light)
                    style = if (isNext) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }

        if (reward.showLabelBelow) {
            GoldRewardLabel(text = reward.label, modifier = Modifier.width(42.dp))
        } else {
            Text(
                text = " ",
                modifier = Modifier.width(42.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * 원 아래 골드 금액 글자 ("100G" 등)
 *
 * 큰 글꼴에서 labelSmall 한 줄이 칸 폭보다 넓으면 뒷글자가 다음 줄로 넘어가 1줄 제한에 잘렸다 (#741)
 *   ("100G" 기준 320dp·글꼴 1.8·2.0에서는 "0G", 그 밖의 잘린 조건(411dp·글꼴 2.0 등)에서는 "G"가 잘림 — 수정 전 코드로 돌린 임시 계측 테스트 로그)
 * - labelSmall 그대로 한 줄에 들어가면 수정 전처럼 그리고, 안 들어갈 때만 [GOLD_LABEL_MIN_FONT_SIZE]까지 줄여 한 줄로 맞춘다
 * - 항상 autoSize를 켜지 않는 이유: 들어가는데도 기기 dpi에 따라 한 단계(0.25sp) 작게 나올 수 있다 (PR #734)
 * - 말줄임은 쓰지 않음: 금액 일부가 가려져 보상을 잘못 읽을 수 있다
 * - 칸 폭(width(42.dp), 좁으면 칸에 맞춰 줄어듦)은 그대로라 7칸 배치는 바뀌지 않는다
 * - 들어가는지는 현재 폭에서 재서 정함 (폭이 바뀌면 다시 잼)
 */
@Composable
private fun GoldRewardLabel(text: String, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelSmall
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val fits = !textMeasurer.measure(
            text = text,
            style = style,
            maxLines = 1,
            constraints = Constraints(maxWidth = constraints.maxWidth)
        ).hasVisualOverflow
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            autoSize = if (fits) null else {
                TextAutoSize.StepBased(minFontSize = GOLD_LABEL_MIN_FONT_SIZE, maxFontSize = style.fontSize)
            }
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


// 홈 레벨업 버튼의 비활성처럼 보이기(alpha 0.38f)와 같은 값
private const val OFFLINE_BUTTON_ALPHA = 0.38f
private const val OFFLINE_STATE_DESCRIPTION = "오프라인"

// 골드 글자를 줄일 수 있는 최소 크기. 최대 크기는 labelSmall(11sp)
// 320dp·글꼴 2.0 칸(SM-N986N에서 86px)에 "100G"가 한 줄로 들어가는 크기가 6sp~7sp 사이라 6sp로 둠
// (6sp 75px, 7sp 88px — AttendanceGoldLabelLargeFontTest 작업 중 TextMeasurer로 잰 값)
private val GOLD_LABEL_MIN_FONT_SIZE = 6.sp

private val attendanceRewards: List<AttendanceRewardUi> = (1..28).map { day ->
    when (val reward = AttendanceRewardTable.of(day)) {
        is AttendanceReward.Coin ->
            AttendanceRewardUi(day, "${reward.amount}G", R.drawable.ic_coin, showLabelBelow = true)

        is AttendanceReward.ItemReward ->
            AttendanceRewardUi(day, reward.type.kor, reward.type.resId)

        null -> AttendanceRewardUi(day)
    }
}
