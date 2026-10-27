package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 글꼴 크기를 작게 설정해도 빈 원 테두리가 보이도록 하는 아이콘 최소 크기
private val MinIconSize = 6.dp

// 학습 목표 한 줄 - 완료: 채워진 원 / 미완료: 빈 원 아이콘을 항목 앞에 표시
@Composable
fun StudyGoalItem(
    text: String,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    // 시스템 글꼴 크기 설정에 맞춰 텍스트와 함께 커지도록 sp 단위로 받음
    iconSize: TextUnit = 9.sp,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val color = if (isCompleted) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
    // 여러 줄로 줄바꿈돼도 아이콘이 첫 줄 가운데에 오도록 첫 줄 높이만큼의 영역에 배치
    // 기본 LineHeightStyle은 첫 줄 위 여백을 잘라내 글자가 위로 치우치므로, 여백을 유지하고 줄 안에서 가운데 정렬
    val textStyle = style.copy(
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
    // em 단위는 toDp() 변환이 불가능해 크래시가 나므로 sp일 때만 변환
    val iconSizeDp = with(LocalDensity.current) {
        (if (iconSize.isSp) iconSize.toDp() else MinIconSize).coerceAtLeast(MinIconSize)
    }
    val firstLineHeight = with(LocalDensity.current) {
        if (style.lineHeight.isSp) style.lineHeight.toDp() else iconSizeDp
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.height(maxOf(firstLineHeight, iconSizeDp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.Circle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (isCompleted) "완료" else "미완료",
                tint = color,
                modifier = Modifier.size(iconSizeDp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = textStyle,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}
