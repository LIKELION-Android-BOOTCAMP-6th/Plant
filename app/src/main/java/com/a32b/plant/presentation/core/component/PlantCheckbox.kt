package com.a32b.plant.presentation.core.component

import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.a32b.plant.presentation.theme.LocalIsDarkTheme

// 앱 공통 체크박스 색상
// 미체크 테두리: 라이트는 기존 outline 유지, 다크는 outline이 카드 배경(secondaryContainer)과 거의 같아 안 보이므로 onSurface 사용
@Composable
fun plantCheckboxColors(): CheckboxColors = CheckboxDefaults.colors(
    uncheckedColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.outline
    }
)
