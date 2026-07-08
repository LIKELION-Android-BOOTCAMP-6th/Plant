package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
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
fun getLogoImage(){
    Image(
        painter = painterResource(id = R.drawable.logo_plant),
        contentDescription = "로고 사진",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(55.dp).padding(10.dp).clip(CircleShape)
    )
}