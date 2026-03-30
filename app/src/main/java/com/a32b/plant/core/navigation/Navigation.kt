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
import com.a32b.plant.ui.feature.community.ui.CommunityDetailScreen
import com.a32b.plant.ui.feature.community.ui.CommunityListScreen
import com.a32b.plant.ui.feature.community.ui.CommunityPostScreen
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.feature.auth.ui.SignInScreen
import com.a32b.plant.ui.feature.auth.ui.SignUpScreen
import com.a32b.plant.ui.feature.home.ui.HomeScreen
import com.a32b.plant.ui.feature.home.ui.NewBornTreeScreen
import com.a32b.plant.ui.feature.mypage.ui.MyPageArchiveScreen
import com.a32b.plant.ui.feature.mypage.ui.MyPageScreen
import com.a32b.plant.ui.feature.mypage.ui.MyPageSettingScreen
import com.a32b.plant.ui.feature.splash.SplashViewModel
import com.a32b.plant.ui.feature.studyPalnDtail.ui.StudyPlanDetailScreen
import com.a32b.plant.ui.feature.studying.ui.StudyResultScreen
import com.a32b.plant.ui.feature.studying.ui.StudyingScreen

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel) {

    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController, startDestination = startRoute){

            composable<Routes.HomeMain> { HomeScreen(navController) }
            composable<Routes.Mypage> { MyPageScreen(navController) }
            composable<Routes.MyPageSetting> { MyPageSettingScreen(navController) }
            composable<Routes.MyPageArchive> { MyPageArchiveScreen(navController) }

            composable<Routes.CommunityList> {
                CommunityListScreen(navController)
            }

            composable<Routes.CommunityPost> { backStackEntry ->

                val route = backStackEntry.toRoute<Routes.CommunityPost>()
                CommunityPostScreen(navController, postId = route.postId)
            }


            composable<Routes.CommunityDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.CommunityDetail>()
                val detailVm: CommunityDetailViewModel = viewModel(
                    factory = ViewModelFactory.communityDetailViewModelFactory(route.postId)
                )


                CommunityDetailScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = detailVm,
                    navController = navController
                )
            }

            composable<Routes.Studying> { StudyingScreen(navController) }
            composable<Routes.StudyResult> { StudyResultScreen(navController) }
            composable<Routes.SignIn> { SignInScreen(navController) }
            composable<Routes.SignUp> { SignUpScreen(navController) }
            composable<Routes.NewBornTree> { NewBornTreeScreen(navController) }
            composable<Routes.StudyPlanDetail> { StudyPlanDetailScreen(navController = navController) }
        }
    }
}