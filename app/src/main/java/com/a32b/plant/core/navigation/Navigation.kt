package com.a32b.plant.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.a32b.plant.presentation.auth.ui.SignInScreen
import com.a32b.plant.presentation.community.ui.CommunityActivityScreen
import com.a32b.plant.presentation.community.ui.CommunityDetailScreen
import com.a32b.plant.presentation.community.ui.CommunityListScreen
import com.a32b.plant.presentation.community.ui.CommunityPostScreen
import com.a32b.plant.presentation.feedback.ui.BugReportScreen
import com.a32b.plant.presentation.feedback.ui.ImprovementSuggestionScreen
import com.a32b.plant.presentation.home.ui.HomeScreen
import com.a32b.plant.presentation.mypage.ui.AccountManageScreen
import com.a32b.plant.presentation.mypage.ui.MyPageScreen
import com.a32b.plant.presentation.pot.ui.NewBornTreeScreen
import com.a32b.plant.presentation.pot.ui.PotListScreen
import com.a32b.plant.presentation.report.ReportScreen
import com.a32b.plant.presentation.splash.SplashViewModel
import com.a32b.plant.presentation.studyPlanDetail.ui.StudyPlanDetailScreen
import com.a32b.plant.presentation.studying.ui.StudyResultScreen
import com.a32b.plant.presentation.studying.ui.StudyingScreen

private const val NAV_TRANSITION_DURATION_MS = 400

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel) {

    val splashUiState by viewModel.uiState.collectAsState()
    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController,
            startDestination = startRoute,
            // 뒤로가기 제스처(Predictive Back)는 손가락 위치만큼 전환을 진행시키므로,
            // 제스처 방향(왼쪽 → 오른쪽)과 같은 가로 slide로 통일해 버튼·하단바·제스처 복귀가 같은 전환으로 보이게 한다 (#389)
            // - 이동: 새 화면이 오른쪽에서 들어오고 이전 화면은 왼쪽으로 나감
            // - 복귀(popBackStack, 제스처, 하단바 홈 탭처럼 popUpTo로 위 화면을 지우는 이동): 반대 방향
            enterTransition = { slideIntoContainer(SlideDirection.Start, tween(NAV_TRANSITION_DURATION_MS)) },
            exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(NAV_TRANSITION_DURATION_MS)) },
            popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(NAV_TRANSITION_DURATION_MS)) },
            popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(NAV_TRANSITION_DURATION_MS)) }
        ) {

            composable<Routes.HomeMain> {
                HomeScreen(
                    navController,
                    isSplashFinished = splashUiState.isFirstScreenReady && splashUiState.isSplashExitFinished
                )
            }
            composable<Routes.Mypage> { MyPageScreen(navController) }
            composable<Routes.AccountManage> { AccountManageScreen(navController) }

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
            composable<Routes.NewBornTree> { NewBornTreeScreen(navController) }
            composable<Routes.StudyPlanDetail> { StudyPlanDetailScreen(navController = navController) }

            composable<Routes.PotList> { PotListScreen(navController = navController) }
            composable<Routes.Report> { ReportScreen(navController) }
            composable<Routes.CommunityActivity> { CommunityActivityScreen(navController) }
            composable<Routes.BugReport> { BugReportScreen(navController) }
            composable<Routes.ImprovementSuggestion> { ImprovementSuggestionScreen(navController) }
        }
    }
}
