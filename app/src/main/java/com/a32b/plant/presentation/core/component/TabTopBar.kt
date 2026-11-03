package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.a32b.plant.presentation.theme.PlantTheme

// 하단바 탭 화면(커뮤니티, 화분, 리포트, 마이페이지) 공통 앱바. 홈은 HomeTopBar를 사용한다.
// 높이(64dp)와 좌우 여백(16dp)은 CenterAlignedTopAppBar 기본값을 따른다.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabTopBar(
    title: String,
    modifier: Modifier = Modifier,
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
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview(showBackground = true, name = "타이틀만")
@Composable
private fun TabTopBarTitleOnlyPreview() {
    PlantTheme(darkTheme = false) {
        TabTopBar(title = "리포트")
    }
}

@Preview(showBackground = true, name = "텍스트 버튼")
@Composable
private fun TabTopBarTextActionPreview() {
    PlantTheme(darkTheme = false) {
        TabTopBar(title = "커뮤니티") {
            TextButton(onClick = {}) {
                Text(
                    "내 활동",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "아이콘 버튼")
@Composable
private fun TabTopBarIconActionPreview() {
    PlantTheme(darkTheme = false) {
        TabTopBar(title = "나만의 정원") {
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "화분 추가")
            }
        }
    }
}

@Preview(showBackground = true, name = "긴 타이틀", widthDp = 320)
@Composable
private fun TabTopBarLongTitlePreview() {
    PlantTheme(darkTheme = false) {
        TabTopBar(title = "타이틀이 아주 길어져도 오른쪽 요소와 겹치지 않아야 합니다") {
            TextButton(onClick = {}) {
                Text("내 활동", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Preview(showBackground = true, name = "다크모드")
@Composable
private fun TabTopBarDarkPreview() {
    PlantTheme(darkTheme = true) {
        TabTopBar(title = "커뮤니티") {
            TextButton(onClick = {}) {
                Text(
                    "내 활동",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
