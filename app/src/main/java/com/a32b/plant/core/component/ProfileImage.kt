package com.a32b.plant.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a32b.plant.R
import com.a32b.plant.core.util.PlantLevel

@Composable
fun ProfileImage(level: String, size: Int){

    val levelInt = level.toIntOrNull() ?: 0
    // 레벨 변경에 따른 이미지 변경
    val imageRes = when(levelInt){
        1 -> R.drawable.ic_pot_lv1 //
        2 -> R.drawable.ic_pot_lv2 // 30시간 이상
        3 -> R.drawable.ic_pot_lv3 // 50시간 이상
        4 -> R.drawable.ic_pot_lv4 // 100시간 이상
        else -> R.drawable.ic_pot_lv0 // 기본 (0~9시간)
    }
    Image(
        painter = painterResource(id = PlantLevel.getPlantImage(level)),
        contentDescription = "화분 성장 단계 : $level",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp).clip(CircleShape)
    )

}