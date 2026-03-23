package com.a32b.plant.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.a32b.plant.R

// Set of Material typography styles to start with
val basic = FontFamily(
    Font(R.font.light)
)
val bold = FontFamily(Font(R.font.medium))
val title = FontFamily(Font(R.font.bold))
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = title,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        color = primary

    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

)