package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a32b.plant.domain.type.PlantLevel
import com.a32b.plant.presentation.core.extension.resId

@Composable
fun ProfileImage(level: String, size: Int, modifier: Modifier = Modifier){

    val levelInt = level.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

    // resId의 else가 logo_plant이므로 매핑 밖 값은 넘기기 전에 LV0으로 처리
    val plantLevel = PlantLevel.entries.firstOrNull { it.label == "lv.$levelInt" } ?: PlantLevel.LV0

    Image(
        painter = painterResource(id = plantLevel.resId),
        contentDescription = "화분 성장 단계 : ${plantLevel.label}",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
    )

}