package com.a32b.plant.presentation.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.a32b.plant.presentation.theme.LocalIsDarkTheme

/**
 * 화면 전체를 막아야 하는 처리(탈퇴, 피드백 제출 등)에 쓰는 전체 화면 로딩 덮개.
 *
 * 화면 안에 두는 [LoadingBox]와 달리 별도 창(Dialog)이라 상단바·하단바 위까지 덮고,
 * 바깥 터치·뒤로가기로 닫히지 않는다. 처리가 끝나면 호출하는 쪽에서 컴포지션에서 빼서 닫는다.
 */
@Composable
fun LoadingDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            // 두 값이 모두 false여야 창이 기본 폭 제한 없이 상태바·내비게이션 바까지 화면 전체 크기가 된다
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val isDarkTheme = LocalIsDarkTheme.current
        SideEffect {
            if (window == null) return@SideEffect
            // Dialog 창 기본 어둡게(dim)를 끈다. 켜 두면 LoadingBox 덮개와 겹쳐 화면 안 LoadingBox보다 어두워진다
            window.setDimAmount(0f)
            // 이 창이 상태바까지 덮으므로 아이콘 색도 테마에 맞춰 지정한다
            // (지정하지 않았을 때 Android 13 실기기 라이트 테마에서 흰 아이콘이 회색 덮개 위에 표시됨, #648)
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkTheme
        }
        LoadingBox()
    }
}
