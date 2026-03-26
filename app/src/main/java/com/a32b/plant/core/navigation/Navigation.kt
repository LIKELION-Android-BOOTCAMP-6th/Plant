package com.a32b.plant.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.auth.ui.SignInScreen
import com.a32b.plant.ui.feature.auth.ui.SignUpScreen
import com.a32b.plant.ui.feature.community.ui.CommunityListScreen
import com.a32b.plant.ui.feature.community.ui.CommunityPostScreen
import com.a32b.plant.ui.feature.community.ui.CommunityDetailScreen
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.feature.home.ui.HomeScreen
import com.a32b.plant.ui.feature.home.ui.NewBornTreeScreen
import com.a32b.plant.ui.feature.mypage.ui.MypageScreen
import com.a32b.plant.ui.feature.mypage.ui.MypageSetting
import com.a32b.plant.ui.feature.splash.SplashViewModel
import com.a32b.plant.ui.feature.studying.ui.StudyResultScreen
import com.a32b.plant.ui.feature.studying.ui.StudyingScreen

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel) {

    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController, startDestination = startRoute) {
            composable<Routes.HomeMain> { HomeScreen(navController) }
            composable<Routes.Mypage> { MypageScreen(navController) }
            composable<Routes.MypageSetting> { MypageSetting(navController) }

            // 1. 커뮤니티 리스트
            composable<Routes.CommunityList> { CommunityListScreen(navController) }

            // 2. 커뮤니티 글쓰기
            composable<Routes.CommunityPost> {

                CommunityPostScreen(navController)
            }

            composable<Routes.CommunityDetail> {

                CommunityDetailScreen(navController)
            }

            composable<Routes.Studying> { StudyingScreen(navController) }
            composable<Routes.StudyResult> { StudyResultScreen(navController) }
            composable<Routes.SignIn> { SignInScreen(navController) }
            composable<Routes.SignUp> { SignUpScreen(navController) }
            composable<Routes.NewBornTree> { NewBornTreeScreen(navController) }
        }
    }
}