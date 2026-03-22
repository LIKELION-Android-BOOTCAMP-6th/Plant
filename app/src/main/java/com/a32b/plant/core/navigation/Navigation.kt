package com.a32b.plant.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.a32b.plant.ui.feature.community.CommunityListScreen
import com.a32b.plant.ui.feature.home.HomeScreen
import com.a32b.plant.ui.feature.mypage.MypageScreen
import com.a32b.plant.ui.feature.splash.SplashScreen

@Composable
fun PlantAppNavigation(navController: NavHostController){

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable<Routes.Splash> {
            SplashScreen(
                onCheckComplete = {destination ->
                    navController.navigate(destination){
                        popUpTo(Routes.Splash){inclusive = true}
                    }

                }
            )
        }
        composable<Routes.HomeMain> {
            HomeScreen(navController)
        }
        composable<Routes.CommunityList> {
            CommunityListScreen(navController)
        }
        composable<Routes.Mypage> {
            MypageScreen(navController)
        }
    }

}