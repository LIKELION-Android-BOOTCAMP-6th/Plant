package com.a32b.plant.presentation.auth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.LocalIsDarkTheme
import com.a32b.plant.presentation.theme.googleButtonBorderDark
import com.a32b.plant.presentation.theme.googleButtonBorderLight
import com.a32b.plant.presentation.theme.googleButtonContainerDark
import com.a32b.plant.presentation.theme.googleButtonContainerLight
import com.a32b.plant.presentation.theme.googleButtonContentDark
import com.a32b.plant.presentation.theme.googleButtonContentLight

/**
 * 구글 브랜딩 가이드(pill 형태)를 따르는 "Google로 계속하기" 버튼
 * - 라이트: 흰 배경 + 회색 테두리 / 다크: 검정 배경 + 회색 테두리
 * - Surface(onClick)로 리플과 Button Role(접근성)을 제공
 */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val containerColor = if (isDark) googleButtonContainerDark else googleButtonContainerLight
    val borderColor = if (isDark) googleButtonBorderDark else googleButtonBorderLight
    val contentColor = if (isDark) googleButtonContentDark else googleButtonContentLight

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 브랜딩 가이드 지정 폰트(Roboto Medium) 사용을 위해 시스템 기본 폰트 적용
            Text(
                text = "Google로 계속하기",
                style = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = contentColor
            )
        }
    }
}
