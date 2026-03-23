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
import com.a32b.plant.core.util.PlantImageMapper

@Composable
fun ProfileImage(level: String, size: Int){
    Image(
        painter = painterResource(id = PlantImageMapper.getPlantImage(level)),
        contentDescription = "프로필 이미지 $level",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size.dp).clip(CircleShape)
    )

}