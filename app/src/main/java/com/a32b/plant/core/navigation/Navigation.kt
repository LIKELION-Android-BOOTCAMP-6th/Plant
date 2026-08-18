package com.a32b.plant.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.a32b.plant.presentation.auth.ui.SignInScreen
import com.a32b.plant.presentation.auth.ui.SignUpScreen
import com.a32b.plant.presentation.community.ui.CommunityDetailScreen
import com.a32b.plant.presentation.community.ui.CommunityListScreen
import com.a32b.plant.presentation.community.ui.CommunityPostScreen
import com.a32b.plant.presentation.home.ui.HomeScreen
import com.a32b.plant.presentation.mypage.ui.MyPageScreen
import com.a32b.plant.presentation.mypage.ui.MyPageSettingScreen
import com.a32b.plant.presentation.pot.ui.NewBornTreeScreen
import com.a32b.plant.presentation.pot.ui.PotListScreen
import com.a32b.plant.presentation.report.ReportScreen
import com.a32b.plant.presentation.splash.SplashViewModel
import com.a32b.plant.presentation.studyPlanDetail.ui.StudyPlanDetailScreen
import com.a32b.plant.presentation.studying.ui.StudyResultScreen
import com.a32b.plant.presentation.studying.ui.StudyingScreen

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel) {

    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController,
            startDestination = startRoute,
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { fadeIn(animationSpec = tween(400)) },
            popExitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {

            composable<Routes.HomeMain> { HomeScreen(navController) }
            composable<Routes.Mypage> { MyPageScreen(navController) }
            composable<Routes.MyPageSetting> { MyPageSettingScreen(navController) }

            composable<Routes.CommunityList> {
                CommunityListScreen(navController)
            }

            composable<Routes.CommunityPost> { CommunityPostScreen(navController) }

            composable<Routes.CommunityDetail> {
                CommunityDetailScreen(navController)
            }

            composable<Routes.Studying> { StudyingScreen(navController) }
            composable<Routes.StudyResult> { StudyResultScreen(navController) }
            composable<Routes.SignIn> { SignInScreen(navController) }
            composable<Routes.SignUp> { SignUpScreen(navController) }
            composable<Routes.NewBornTree> { NewBornTreeScreen(navController) }
            composable<Routes.StudyPlanDetail> { StudyPlanDetailScreen(navController = navController) }

            composable<Routes.PotList> { PotListScreen(navController = navController) }
            composable<Routes.Report> { ReportScreen(navController) }
        }
    }
}
