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
import com.a32b.plant.ui.feature.mypage.ui.MypageScreen
import com.a32b.plant.ui.feature.mypage.ui.MypageSetting
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
            composable<Routes.Mypage> { MypageScreen(navController) }
            composable<Routes.MypageSetting> { MypageSetting(navController) }

            composable<Routes.CommunityList> {
                CommunityListScreen(navController)
            }

            composable<Routes.CommunityPost> {
                val postVm: CommunityPostViewModel = viewModel(
                    factory = ViewModelFactory.communityPostViewModelFactory
                )
                CommunityPostScreen(navController, postVm)
            }

            // ✅ 2. 상세 화면 조립 방식 수정
            composable<Routes.CommunityDetail> { backStackEntry ->
                val route = backStackEntry.toRoute<Routes.CommunityDetail>()
                val detailVm: CommunityDetailViewModel = viewModel(
                    factory = ViewModelFactory.communityDetailViewModelFactory(route.postId)
                )

                // navController를 직접 주는 게 아니라, '뒤로가기 버튼을 눌렀을 때의 동작'을 전달합니다.
                CommunityDetailScreen(
                    onBack = { navController.popBackStack() }, // 👈 이렇게 써야 에러가 안 납니다!
                    viewModel = detailVm
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