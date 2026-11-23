package com.a32b.plant.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
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
import com.a32b.plant.presentation.mypage.ui.BlockedUsersScreen
import com.a32b.plant.presentation.mypage.ui.MyPageScreen
import com.a32b.plant.presentation.mypage.ui.OpenSourceLicenseScreen
import com.a32b.plant.presentation.pot.ui.NewBornTreeScreen
import com.a32b.plant.presentation.pot.ui.PotListScreen
import com.a32b.plant.presentation.report.ReportScreen
import com.a32b.plant.presentation.splash.SplashViewModel
import com.a32b.plant.presentation.studyPlanDetail.ui.StudyPlanDetailScreen
import com.a32b.plant.presentation.studying.ui.StudyResultScreen
import com.a32b.plant.presentation.studying.ui.StudyingScreen

private const val NAV_TRANSITION_DURATION_MS = 400
private const val TAB_FADE_DURATION_MS = 200

/** 출발·도착 화면이 모두 하단바 탭 화면인 전환인지 (#674) */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isBetweenBottomTabs(): Boolean =
    initialState.destination.isBottomTab() && targetState.destination.isBottomTab()

@Composable
fun PlantAppNavigation(navController: NavHostController, viewModel: SplashViewModel) {

    val splashUiState by viewModel.uiState.collectAsState()
    val destination by viewModel.destination.collectAsState()
    destination?.let { startRoute ->
        NavHost(navController = navController,
            startDestination = startRoute,
            // 뒤로가기 제스처(Predictive Back)는 끈 거리만큼 pop 전환을 진행시키므로,
            // 왼쪽 가장자리 제스처 방향(왼쪽 → 오른쪽)과 같은 가로 slide로 통일해 버튼·하단바·제스처 복귀가 같은 전환으로 보이게 한다 (#389)
            // - 이동: 새 화면이 오른쪽에서 들어오고 이전 화면은 왼쪽으로 나감
            // - 복귀: 반대 방향. popBackStack, 뒤로가기 제스처, popUpTo + launchSingleTop으로 이미 있는 화면으로 돌아가는 이동 (하단바 홈 탭은 아래 예외로 페이드)
            //   (popUpTo 뒤에 다른 화면으로 navigate하면 이동 전환이 된다. 예: 공부 화면의 popUpTo(0) 후 홈 이동)
            // - 한계: NavHost는 제스처를 시작한 가장자리를 전환에 넘기지 않아, 오른쪽 가장자리 제스처에서도 화면이 오른쪽으로 밀린다 (손가락과 반대 방향)
            // - 예외: 출발·도착이 모두 탭 화면이면 짧은 페이드 (#674)
            //   하단바 탭 이동은 popUpTo(HomeMain) 후 navigate라 홈 탭만 복귀 방향이 돼 슬라이드 방향이 탭 순서와 맞지 않는다.
            //   탭 화면에서 뒤로가기로 홈에 돌아가는 것도 탭 ↔ 탭이라 페이드. 상세 화면이 끼면 출발·도착 중 하나가 탭이 아니라 슬라이드 그대로
            enterTransition = {
                if (isBetweenBottomTabs()) fadeIn(tween(TAB_FADE_DURATION_MS))
                else slideIntoContainer(SlideDirection.Start, tween(NAV_TRANSITION_DURATION_MS))
            },
            exitTransition = {
                if (isBetweenBottomTabs()) fadeOut(tween(TAB_FADE_DURATION_MS))
                else slideOutOfContainer(SlideDirection.Start, tween(NAV_TRANSITION_DURATION_MS))
            },
            popEnterTransition = {
                if (isBetweenBottomTabs()) fadeIn(tween(TAB_FADE_DURATION_MS))
                else slideIntoContainer(SlideDirection.End, tween(NAV_TRANSITION_DURATION_MS))
            },
            popExitTransition = {
                if (isBetweenBottomTabs()) fadeOut(tween(TAB_FADE_DURATION_MS))
                else slideOutOfContainer(SlideDirection.End, tween(NAV_TRANSITION_DURATION_MS))
            }
        ) {

            composable<Routes.HomeMain> {
                HomeScreen(
                    navController,
                    isSplashFinished = splashUiState.isFirstScreenReady && splashUiState.isSplashExitFinished
                )
            }
            composable<Routes.Mypage> { MyPageScreen(navController) }
            composable<Routes.AccountManage> { AccountManageScreen(navController) }
            composable<Routes.OpenSourceLicense> { OpenSourceLicenseScreen(navController) }
            composable<Routes.BlockedUsers> { BlockedUsersScreen(navController) }

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
