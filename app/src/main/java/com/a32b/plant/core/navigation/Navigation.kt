package com.a32b.plant.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.a32b.plant.ui.feature.auth.ui.SignInScreen
import com.a32b.plant.ui.feature.auth.ui.SignUpScreen
import com.a32b.plant.ui.feature.community.ui.CommunityListScreen
import com.a32b.plant.ui.feature.community.ui.CommunityPostScreen
import com.a32b.plant.ui.feature.home.ui.HomeScreen
import com.a32b.plant.ui.feature.mypage.ui.MypageScreen
import com.a32b.plant.ui.feature.splash.SplashViewModel
import com.a32b.plant.ui.feature.studying.ui.StudyingScreen

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel){

    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController, startDestination = startRoute){
            composable<Routes.HomeMain> { HomeScreen(navController) }
            composable<Routes.Mypage> { MypageScreen(navController) }
            composable<Routes.CommunityList> { CommunityListScreen(navController) }
            composable<Routes.Studying> { StudyingScreen(navController) }
            composable<Routes.SignIn> { SignInScreen(navController) }
            composable<Routes.SignUp> { SignUpScreen(navController) }
            // 🧭 새 화면(글쓰기 상세) 길 만들기 추가!
            composable<Routes.CommunityPost> { CommunityPostScreen(navController) }
        }
    }

}