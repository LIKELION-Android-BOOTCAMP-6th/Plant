package com.a32b.plant.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// 색 역할 값 테스트(PlantColorSchemeTest)에서 읽을 수 있게 internal (#643)
internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF627347),
    onPrimary = Color.Black,

    background = Color(0xFF2D2D2D),
    onBackground = Color(0xFFF1F1F1),

    //카드/ 기본 다이얼로그 배경색
    primaryContainer = Color(0xFF414141),

    //하단바 배경색
    surface = Color(0xFF8E9872),

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
    outline = Color(0xFF707070),

    // 아래는 Material 기본값(보라 계열)이 섞이지 않도록 정의하는 역할 (#643). 새 색 없이 위 값을 재사용
    //카드/다이얼로그(primaryContainer)·사각형 버튼(secondaryContainer) 위 글자 = 메인 폰트 색
    onPrimaryContainer = Color(0xFFF1F1F1),
    onSecondaryContainer = Color(0xFFF1F1F1),

    //서브 폰트 색 (텍스트필드 placeholder, 라디오 비선택, 시트 손잡이 등 M3 기본 사용처 포함)
    onSurfaceVariant = Color(0xFFB8B8B8),

    //error 위 글자: 흰색은 대비 약 3.8:1이라 검정(약 5.6:1)
    onError = Color.Black,
    errorContainer = Color(0xFFCF6262),
    onErrorContainer = Color.Black,

    //M3 컴포넌트 기본 배경 (DropdownMenu=surfaceContainer, ModalBottomSheet=surfaceContainerLow, Card·Switch=surfaceContainerHighest)
    surfaceContainerLowest = Color(0xFF2D2D2D),
    surfaceContainerLow = Color(0xFF2D2D2D),
    surfaceContainer = Color(0xFF414141),
    surfaceContainerHighest = Color(0xFF414141),

    //구분선(M3 Divider 기본값)
    outlineVariant = Color(0xFF707070),

    //다이얼로그·시트 뒤 어두운 막
    scrim = Color.Black
)

internal val LightColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,

    background = background,
    onBackground = fontColor,

    //카드/ 기본 다이얼로그 배경색
    primaryContainer = Color.White,

    //하단바 배경색
    surface = Color(0xFFC0C9A5),

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

    //비활성화 — F8F6F6은 배경(FFFFFD)과 거의 같아 비활성 버튼이 안 보여 sub3(D9D9D9)로 변경 (#643)
    tertiaryContainer = sub3,

    error = Color(0xFFB3261E),

    //메인 폰트 색
    onSurface = fontColor,

    //서브 폰트색, 힌트용
    onSecondary = fontColorSub,

    //테두리, 3순위 극소부위 힌트용
    outline = Color.LightGray,

    // 아래는 Material 기본값(보라 계열)이 섞이지 않도록 정의하는 역할 (#643). 새 색 없이 위 값을 재사용
    //카드/다이얼로그(primaryContainer)·사각형 버튼(secondaryContainer) 위 글자 = 메인 폰트 색
    onPrimaryContainer = fontColor,
    onSecondaryContainer = fontColor,

    //서브 폰트 색 (텍스트필드 placeholder, 라디오 비선택, 시트 손잡이 등 M3 기본 사용처 포함)
    onSurfaceVariant = fontColorSub,

    onError = Color.White,
    errorContainer = Color(0xFFB3261E),
    onErrorContainer = Color.White,

    //M3 컴포넌트 기본 배경 (DropdownMenu=surfaceContainer, ModalBottomSheet=surfaceContainerLow, Card·Switch=surfaceContainerHighest)
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = background,
    surfaceContainer = Color.White,
    surfaceContainerHighest = sub2,

    //구분선(M3 Divider 기본값)
    outlineVariant = Color.LightGray,

    //다이얼로그·시트 뒤 어두운 막
    scrim = Color.Black
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
