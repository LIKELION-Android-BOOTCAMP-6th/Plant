package com.a32b.plant.ui.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.a32b.plant.core.navigation.Routes

@Composable
fun SplashScreen(onCheckComplete: (Routes) -> Unit, viewModel: SplashViewModel = viewModel()){
    LaunchedEffect(Unit) {
        viewModel.destination.collect { routes ->
            onCheckComplete(routes)
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.White))
}

