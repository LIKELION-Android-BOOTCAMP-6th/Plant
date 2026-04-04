package com.a32b.plant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.a32b.plant.ui.theme.background

private val DarkColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = Color(0xFF000000),

    background = Color(0xFF2D2D2D),
    onBackground = Color(0xFFE6E1D6),

    //다이얼로그 백, 커뮤니티 포스트 입력창, 커뮤니티 디테일 내용
    //기본 바탕이 흰 색인 것
    secondaryContainer = Color(0xFF757575),
    onSecondaryContainer = Color.White,

    onSurface = Color(0xFFE6E1E5),

    //홈의 카드, 개별학습기록 백, 공부중 백
    //기본 바탕이 연한 회색인 것
    surfaceVariant = Color(0xFF4A4A4A),
    onSurfaceVariant = Color(0xFFB0B0B0),

    secondary = Color(0xFFD8B787),
    tertiary = Color(0xFF44474E),


)

private val LightColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,

    background = background,
    onBackground = Color(0xFF1C1B1F),

    secondaryContainer = Color.White,
    onSecondaryContainer = fontColor,

    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF8F6F6),
    onSurfaceVariant = Color(0xFF44474E),

    secondary = sub1,
    onSecondary = fontColor,


    tertiary = sub2,


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PlantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}