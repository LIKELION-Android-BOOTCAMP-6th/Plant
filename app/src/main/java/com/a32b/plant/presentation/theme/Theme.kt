package com.a32b.plant.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = primary.copy(0.5f),
    onPrimary = Color.Black,

    background = Color(0xFF2D2D2D),
    onBackground = Color(0xFFF1F1F1),

    //카드/ 기본 다이얼로그 배경색
    primaryContainer = Color.Black,

    //하단바 배경색
    surface = Color(0xFFD8B787),

    //보조 배경
    surfaceVariant = Color(0xFF9BA199),

    //텍스트필드 배경색
    surfaceContainerHigh = textFieldBackgroundDark,

    //옅은 초록색, 홈 메인 카드 배경
    secondary = Color(0xFF6E796C),

    //사각형 버튼
    secondaryContainer = Color(0xFF757575),

    //진한 초록색
    tertiary = Color(0xFFEBF7E4),

    //비활성화
    tertiaryContainer = Color(0xFF4a4a4a),

    error = Color(0xFFCF6262),

    //메인 폰트 색
    onSurface = Color(0xFFF1F1F1),

    //서브 폰트색, 힌트용
    onSecondary = Color(0xFFB8B8B8),

    //테두리, 3순위 극소부위 힌트용
    outline = Color(0xFF707070)

)

private val LightColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,

    background = background,
    onBackground = fontColor,

    //카드/ 기본 다이얼로그 배경색
    primaryContainer = Color.White,

    //하단바 배경색
    surface = bottom,

    //보조 배경
    surfaceVariant = sub2,

    //텍스트필드 배경색
    surfaceContainerHigh = textFieldBackground,

    //옅은 초록색, 홈 메인 카드 배경
    secondary = sub_green1,

    //사각형 버튼
    secondaryContainer = Color.White,

    //진한 초록색
    tertiary = sub_green2,

    //비활성화
    tertiaryContainer = Color(0xFFF8F6F6),

    error = Color(0xFFB3261E),

    //메인 폰트 색
    onSurface = fontColor,

    //서브 폰트색, 힌트용
    onSecondary = fontColorSub,

    //테두리, 3순위 극소부위 힌트용
    outline = Color.LightGray
)

@Composable
fun PlantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = plantTypography(colorScheme),
            content = content
        )
    }

}
