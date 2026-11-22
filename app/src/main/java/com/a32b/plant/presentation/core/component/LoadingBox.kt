package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

// 덮개 투명도: M3 BottomSheetDefaults.ScrimColor와 같은 값 (material3 1.4.0 ScrimTokens.ContainerOpacity)
internal const val LOADING_SCRIM_ALPHA = 0.32f

@Composable
fun LoadingBox(modifier: Modifier = Modifier.fillMaxSize()){
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = LOADING_SCRIM_ALPHA))
            .pointerInput(Unit){ detectTapGestures {  } }, //백그라운드 클릭 불가하게 처리
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}
