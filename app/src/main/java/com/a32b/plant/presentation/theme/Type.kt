package com.a32b.plant.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import com.a32b.plant.R

// Set of Material typography styles to start with
val basic = FontFamily(
    Font(R.font.light)
)
val bold = FontFamily(Font(R.font.medium))
val title = FontFamily(Font(R.font.bold))

@Composable
fun plantTypography(colorScheme: ColorScheme): Typography{
    return Typography(
        //primary위에 거대 로고 표시용
        headlineLarge = TextStyle(
            fontFamily = title,
            fontSize = 40.sp,
            color = colorScheme.onPrimary
        ),
        //프라이머리 로고 표현 시
        headlineMedium = TextStyle(
            fontFamily = title,
            fontSize = 22.sp,
            color = colorScheme.primary
        ),
        //앱바 타이틀(헤더)용
        displayLarge = TextStyle(
            fontFamily = title,
            fontSize = 22.sp,
            lineHeight = 36.sp,
            color = colorScheme.onSurface
        ),
        //타이틀 부분 - 커뮤니티 디테일의 제목, 학습계획창의 제목, 마이페이지(닉네임, 버튼들, 창 제목) 등
        titleLarge = TextStyle(
            fontFamily = title,
            fontSize = 30.sp,
            color = colorScheme.onSurface
        ),
        //기본 글씨 볼드용
        titleMedium = TextStyle(
            fontFamily = bold,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSurface
        ),

        //작은 글씨 강조용 - 홈의 날짜, 커뮤니티 리스트의 글제목, 다이얼로그 제목 등
        titleSmall = TextStyle(
            fontFamily = bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = colorScheme.onSurface
        ),
        //스타일 미지정 걸러내기용
        bodyLarge = TextStyle(
            fontFamily = bold,
            fontStyle = FontStyle.Italic
        ),
        //기본 글씨 - 커뮤니티의 게시글 본문 등 기타 모든 일반 글씨 작성 시
        bodyMedium = TextStyle(
            fontFamily = basic,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSurface
        ),
        //작은 글씨
        bodySmall = TextStyle(
            fontFamily = basic,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = colorScheme.onSurface
        ),
        //버튼용 기본 텍스트
        labelLarge = TextStyle(
            fontFamily = basic,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSurface
        ),
        //힌트용
        labelMedium = TextStyle(
            fontFamily = basic,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = colorScheme.onSecondary
        ),
    )
}
