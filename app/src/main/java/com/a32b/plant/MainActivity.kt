package com.a32b.plant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.core.component.BottomBar
import com.a32b.plant.core.navigation.PlantAppNavigation
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.feature.splash.SplashViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SplashViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            //해당 값이 트루일 동안 스플래시 유지
            viewModel.destination.value == null
        }

        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                destination.hasRoute<Routes.HomeMain>() ||
                destination.hasRoute<Routes.CommunityList>() ||
                destination.hasRoute<Routes.Mypage>()
            } ?: false
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (showBottomBar) BottomBar(navController = navController)
                }
            ) {innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)){
                    PlantAppNavigation(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}
