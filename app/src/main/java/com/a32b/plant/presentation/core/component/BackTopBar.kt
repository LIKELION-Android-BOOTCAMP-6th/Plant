package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.PlantTheme

// 뒤로가기가 있는 하위 화면 공통 앱바. 하단바 탭 화면은 TabTopBar, 홈은 HomeTopBar를 사용한다.
// 높이·좌우 여백은 TabTopBar와 같은 CenterAlignedTopAppBar 기본값을 따르고, 제목은 가운데 정렬한다.
// 공부 기록 상세의 공유 모드처럼 뒤로가기 자리에 다른 아이콘이 필요하면 backIcon·backContentDescription을 바꾼다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backEnabled: Boolean = true,
    backIcon: Painter = painterResource(R.drawable.ic_backbtn),
    backContentDescription: String = "뒤로가기",
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack, enabled = backEnabled) {
                Icon(
                    painter = backIcon,
                    contentDescription = backContentDescription,
                    modifier = Modifier.size(19.dp)
                )
            }
        },
        actions = {
            // CenterAlignedTopAppBar는 actions를 앱바 전체 폭으로 재서, actions가 넓으면 제목 자리가 0이 되고
            // 더 넓으면 뒤로가기까지 덮는다 (material3 1.4.0 AppBar.kt TopAppBarMeasurePolicy).
            // 그래서 actions 폭을 "앱바 폭 - 뒤로가기 영역 - 제목 칸 최소 폭"까지로 제한한다.
            // 상한보다 넓은 actions는 안에서 스스로 줄여야 한다 (예: 내 활동 선택 모드의 삭제 버튼 문구 말줄임)
            Row(
                modifier = Modifier.layout { measurable, constraints ->
                    val reserved = (NavigationAreaWidth + TitleMinWidth).roundToPx()
                    // 폭이 정해지지 않은 측정(IntrinsicSize 등)에서는 상한을 두지 않는다.
                    // 무한대에서 빼면 Constraints로 나타낼 수 없는 큰 수가 되어 예외가 난다
                    val maxWidth = if (constraints.hasBoundedWidth) {
                        (constraints.maxWidth - reserved).coerceAtLeast(0)
                    } else {
                        constraints.maxWidth
                    }
                    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

// 뒤로가기 영역: 앱바 왼쪽 여백 4dp(material3 TopAppBarHorizontalPadding) + IconButton 레이아웃 48dp
// (최소 터치 영역 48dp 안에 보이는 버튼 40dp가 가운데 놓임 — material3 1.4.0 IconButton.kt)
private val NavigationAreaWidth = 52.dp

// actions가 넓어도 제목에 남겨 두는 제목 칸 최소 폭. 제목 칸 좌우 안쪽 여백 4dp씩(material3 AppBar.kt)을 빼면
// 글자가 쓸 수 있는 폭은 40dp이고, 글꼴이 아주 크면 이 안에서 말줄임된다
private val TitleMinWidth = 48.dp

@Preview(showBackground = true, name = "타이틀만")
@Composable
private fun BackTopBarTitleOnlyPreview() {
    PlantTheme(darkTheme = false) {
        BackTopBar(title = "계정 관리", onBack = {})
    }
}

@Preview(showBackground = true, name = "텍스트 버튼")
@Composable
private fun BackTopBarTextActionPreview() {
    PlantTheme(darkTheme = false) {
        BackTopBar(title = "내 활동", onBack = {}) {
            TextButton(onClick = {}) {
                Text(
                    "선택",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "긴 타이틀 + 아이콘 3개", widthDp = 320)
@Composable
private fun BackTopBarLongTitlePreview() {
    PlantTheme(darkTheme = false) {
        BackTopBar(title = "[자격증] 정보처리기사 실기 대비 화분", onBack = {}) {
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = "수정하기", modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = "공유", modifier = Modifier.size(19.dp))
            }
            IconButton(onClick = {}) {
                Icon(painterResource(R.drawable.ic_trash), contentDescription = "화분 전체 삭제하기", modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "뒤로가기 비활성")
@Composable
private fun BackTopBarBackDisabledPreview() {
    PlantTheme(darkTheme = false) {
        BackTopBar(title = "새 화분 만들기", onBack = {}, backEnabled = false)
    }
}

@Preview(showBackground = true, name = "다크모드")
@Composable
private fun BackTopBarDarkPreview() {
    PlantTheme(darkTheme = true) {
        BackTopBar(title = "버그 제보", onBack = {})
    }
}
