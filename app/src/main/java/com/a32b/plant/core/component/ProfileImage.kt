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
fun ProfileImage(level: String, size: Int, modifier: Modifier = Modifier){

    val imageRes = when (level) {
        "0" -> R.drawable.ic_pot_lv0
        "1" -> R.drawable.ic_pot_lv1
        "2" -> R.drawable.ic_pot_lv2
        "3" -> R.drawable.ic_pot_lv3
        "4" -> R.drawable.ic_pot_lv4
        "5" -> R.drawable.ic_pot_lv5
        else -> R.drawable.logo_plant     // 예외 상황 대비
    }

    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "화분 성장 단계 : $level",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
    )

}