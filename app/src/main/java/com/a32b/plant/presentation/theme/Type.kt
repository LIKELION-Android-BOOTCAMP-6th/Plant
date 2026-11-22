package com.a32b.plant.presentation.theme

import androidx.compose.material3.ColorScheme
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
        //큰 타이머 숫자 - 학습 결과 화면의 학습 시간 (autoSize 최대 크기로 사용) (#644)
        displayMedium = TextStyle(
            fontFamily = title,
            fontSize = 50.sp,
            color = colorScheme.onSurface
        ),
        //중간 크기 시간 숫자 - 홈 메인카드의 오늘 학습 시간 (#644)
        displaySmall = TextStyle(
            fontFamily = title,
            fontSize = 26.sp,
            color = colorScheme.onSurface
        ),
        //결과 날짜 - 학습 결과 화면 상단의 날짜·시각 (autoSize 최대 크기로 사용) (#644)
        //줄 높이는 원래 쓰던 titleSmall 값(20sp)을 그대로 옮겨 화면 높이가 바뀌지 않게 함
        headlineSmall = TextStyle(
            fontFamily = bold,
            fontSize = 22.sp,
            lineHeight = 20.sp,
            color = colorScheme.onSurface
        ),
        //타이틀 부분 - 커뮤니티 디테일의 제목, 학습계획창의 제목, 마이페이지(닉네임, 버튼들, 창 제목) 등
        titleLarge = TextStyle(
            fontFamily = title,
            fontSize = 22.sp, // 30sp -> 22sp 2026.9.22 zickk-kth
            color = colorScheme.onSurface
        ),
        //기본 글씨 볼드용
        titleMedium = TextStyle(
            fontFamily = bold,
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSurface
        ),

        //작은 글씨 강조용 - 홈의 날짜, 커뮤니티 리스트의 글제목 등 (ConfirmDialog 제목은 titleMedium 사용)
        titleSmall = TextStyle(
            fontFamily = bold,
            fontSize = 12.sp,
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
            fontSize = 15.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSurface
        ),
        //작은 글씨
        bodySmall = TextStyle(
            fontFamily = basic,
            fontSize = 12.sp, // 15sp -> 12sp 2026.9.22 zickk-kth
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
            fontSize = 13.sp, // 15sp -> 13sp 2026.9.22 zickk-kth
            lineHeight = 20.sp,
            color = colorScheme.onSecondary
        ),
        //가장 작은 강조 글씨 (bodySmall 12sp보다 작게) - 홈 버그 제보, 출석판 골드 라벨 (#644)
        labelSmall = TextStyle(
            fontFamily = bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = colorScheme.onSurface
        ),
    )
}

//홈 아이템 칸 아래 이름 (#644) - Typography 기본 항목을 모두 써서 확장 속성으로 정의
//labelSmall(11sp)보다 1sp 작게: 글꼴 2배에서 11sp는 "영양제"가 좁은 칸에 다 들어가지 않고 "영양…"으로 잘렸다
val Typography.homeItemLabel: TextStyle
    get() = labelSmall.copy(fontSize = 10.sp)
