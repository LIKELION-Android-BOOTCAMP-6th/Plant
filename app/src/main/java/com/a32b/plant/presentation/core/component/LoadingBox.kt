package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()){
    Box(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.3f))
            .pointerInput(Unit){ detectTapGestures {  } }, //백그라운드 클릭 불가하게 처리
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.LightGray)
    }
}