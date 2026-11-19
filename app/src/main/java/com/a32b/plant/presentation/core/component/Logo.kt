package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a32b.plant.R

/**
 * logo_plant.png는 위에 스톱워치 꼭지가 있어, 원 부분의 중심이 이미지 중심보다 아래에 있다 (#680)
 * 500px 이미지에서 원이 가장 넓은 줄(폭 340px)이 258~301px → 원 중심 약 280px, 이미지 중심 250px보다 30px(6%) 아래
 * (Pillow로 알파 채널 128 초과 픽셀의 줄별 폭을 재서 확인)
 * PNG는 화분 이미지 매핑 기본값(PlantLevelExt)으로도 쓰이므로 파일은 그대로 두고, 여기서만 위로 옮긴다
 */
private const val LOGO_CIRCLE_CENTER_OFFSET_RATIO = 0.06f

@Composable
fun getLogoImage(){
    // 흰 원 배경과 로고 이미지를 분리: 배경은 칸 가운데에 두고, 로고만 원 중심이 맞도록 위로 옮긴다 (#680)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_plant),
            contentDescription = "로고 사진",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                // 그리는 위치만 옮긴다 (레이아웃 크기·자리는 그대로), 이미지 높이에 비례해 칸 크기가 바뀌어도 같은 비율로 이동
                .graphicsLayer { translationY = -size.height * LOGO_CIRCLE_CENTER_OFFSET_RATIO }
        )
    }
}
