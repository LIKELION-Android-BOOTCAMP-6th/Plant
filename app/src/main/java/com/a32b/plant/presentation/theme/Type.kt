package com.a32b.plant.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.a32b.plant.R

// Set of Material typography styles to start with
val basic = FontFamily(
    Font(R.font.light)
)
val bold = FontFamily(Font(R.font.medium))
val title = FontFamily(Font(R.font.bold))

/*@Composable
fun plantTypography(colorScheme: ColorScheme): Typography{
    return Typography(
        //큰 글씨 강조용 - 홈의 가든, 로그인/회원가입의 로고(글씨색 백그라운드로 변경하기)
        displayLarge = TextStyle(
            fontFamily = title,
            fontSize = 22.sp,
            lineHeight = 36.sp,
            color = primary
        ),
        //타이틀 부분 - 커뮤니티 디테일의 제목, 학습계획창의 제목, 마이페이지(닉네임, 버튼들, 창 제목) 등
        titleLarge = TextStyle(
            fontFamily = bold,
            fontSize = 30.sp,
            color = colorScheme.onSecondaryContainer
        ),

        //작은 글씨 강조용 - 홈의 날짜, 커뮤니티 리스트의 글제목, 다이얼로그 제목 등
        titleSmall = TextStyle(
            fontFamily = bold,
            fontSize = 18.sp,
            color = colorScheme.onSecondaryContainer

        ),
        //기본 글씨 - 커뮤니티의 게시글 본문 등 기타 모든 일반 글씨 작성 시
        bodyMedium = TextStyle(
            fontFamily = basic,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = colorScheme.onSecondaryContainer
        ),
        //작은 글씨, 힌트용 - 홈의 나만의 화분 안내 문구(더 연한 색상으로 변경해도 됨), 커뮤니티 날짜, 마이페이지 총 공부 시간
        bodySmall = TextStyle(
            fontFamily = basic,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = fontColorSub
        )
    )
}*/
val Typography = Typography(
    //큰 글씨 강조용 - 홈의 가든, 로그인/회원가입의 로고(글씨색 백그라운드로 변경하기)
    displayLarge = TextStyle(
        fontFamily = title,
        fontSize = 22.sp,
        lineHeight = 36.sp,
        color = primary
    ),
    //타이틀 부분 - 커뮤니티 디테일의 제목, 학습계획창의 제목, 마이페이지(닉네임, 버튼들, 창 제목) 등
    titleLarge = TextStyle(
        fontFamily = bold,
        fontSize = 30.sp,
        color = fontColor
    ),

    //작은 글씨 강조용 - 홈의 날짜, 커뮤니티 리스트의 글제목, 다이얼로그 제목 등
    titleSmall = TextStyle(
        fontFamily = bold,
        fontSize = 18.sp,
        color = fontColor

    ),
    //기본 글씨 - 커뮤니티의 게시글 본문 등 기타 모든 일반 글씨 작성 시
    bodyMedium = TextStyle(
        fontFamily = basic,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = fontColor
    ),
    //작은 글씨, 힌트용 - 홈의 나만의 화분 안내 문구(더 연한 색상으로 변경해도 됨), 커뮤니티 날짜, 마이페이지 총 공부 시간
    bodySmall = TextStyle(
        fontFamily = basic,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = fontColorSub
    )
)
