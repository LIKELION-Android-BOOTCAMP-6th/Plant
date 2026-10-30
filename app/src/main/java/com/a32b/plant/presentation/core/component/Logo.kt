package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.a32b.plant.R

@Composable
fun getLogoImage(){
    Image(
        painter = painterResource(id = R.drawable.logo_plant),
        contentDescription = "로고 사진",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(8.dp)
    )
}