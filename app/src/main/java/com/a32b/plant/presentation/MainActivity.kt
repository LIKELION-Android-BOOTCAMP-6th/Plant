package com.a32b.plant.presentation

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.core.navigation.PlantAppNavigation
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.session.SessionExpiredObserver
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.presentation.core.component.BottomBar
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.home.viewmodel.HomeViewModel
import com.a32b.plant.presentation.splash.SplashViewModel
import com.a32b.plant.presentation.theme.PlantTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var sessionExpiredObserver: SessionExpiredObserver

    @Inject
    lateinit var signOutUseCase: SignOutUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        // enableEdgeToEdge가 decorView를 만들기 때문에 먼저 호출하면 스플래시 테마(ActionBar 있음)로 창이 구성됨
        val compatSplashScreen = installSplashScreen()
        enableEdgeToEdge()
        compatSplashScreen.setKeepOnScreenCondition {
            //해당 값이 트루일 동안 스플래시 유지
            val uiState = viewModel.uiState.value
            viewModel.destination.value == null || uiState.isLoading || !uiState.isFirstScreenReady
        }
        // 시스템 기본 종료 효과(로고 잔상) 없이 첫 화면으로 전환
        // 라이브러리 리스너는 시스템 바 색을 테마 값으로 덮어쓰므로 플랫폼 API를 직접 사용.
        // Android 12(API 31/32)는 직접 제거하면 시스템 바 설정이 되돌아가서 시스템 기본 전환을 쓴다. (Android 11 이하는 원래 즉시 제거됨)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.splashScreen.setOnExitAnimationListener { splashScreenView ->
                // 로딩이 빨라도 로고 애니메이션은 끝까지 보여준 뒤 제거
                val iconStart = splashScreenView.iconAnimationStart
                val iconDuration = splashScreenView.iconAnimationDuration
                val remainingMillis = if (iconStart != null && iconDuration != null) {
                    Duration.between(Instant.now(), iconStart.plus(iconDuration)).toMillis()
                } else {
                    0L
                }
                splashScreenView.postDelayed({ splashScreenView.remove() }, remainingMillis.coerceAtLeast(0L))
            }
        }

        super.onCreate(savedInstanceState)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            if (uiState.isLoading) return@setContent

            PlantTheme(darkTheme = uiState.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()

                    /** 현재 유저 정보를 못 찾을 시
                    1. 로그아웃(앱 세션 클리어)
                    2. 로그인 화면으로 이동
                    3. 유저 정보 조회 실패 다이얼로그 표출
                     */
                    var sessionExpiredReasonOrdinal by rememberSaveable { mutableStateOf<Int?>(null) }
                    val sessionExpiredReason = sessionExpiredReasonOrdinal?.let { SessionExpiredReason.entries[it] }
                    val lifecycleOwner = LocalLifecycleOwner.current

                    LaunchedEffect(Unit) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            sessionExpiredObserver.event.collect { reason ->
                                val alreadyAtSignIn = navController.currentBackStackEntry
                                    ?.destination?.hasRoute<Routes.SignIn>() == true
                                if (alreadyAtSignIn) return@collect

                                signOutUseCase()

                                navController.navigate(Routes.SignIn) {
                                    popUpTo(0) { inclusive = true }
                                }

                                sessionExpiredReasonOrdinal = reason.ordinal
                            }
                        }
                    }

                    if (sessionExpiredReason != null) {
                        val (text, semiText) = when (sessionExpiredReason) {
                            SessionExpiredReason.DUPLICATE_LOGIN ->
                                "다른 기기에서 로그인되었습니다." to "보안을 위해 자동 로그아웃 됩니다."
                            SessionExpiredReason.USER_NOT_FOUND ->
                                "로그인한 유저의 정보를 찾을 수 없습니다." to "다시 로그인해 주세요."
                        }
                        ConfirmDialog(
                            text = text,
                            semiText = semiText,
                            isSingleBtn = true,
                            confirmBtnText = "확인",
                            onDismiss = {},
                            onConfirm = { sessionExpiredReasonOrdinal = null }
                        )
                    }

                    // HomeMain BackStackEntry가 스택에 있을 때 HomeScreen과 동일한
                    // HomeViewModel 인스턴스를 공유해 recoverySession 상태를 관찰한다.
                    // HomeMain이 스택에 없는 경우(로그인 화면 등) null을 반환해 차단하지 않는다.
                    //
                    // [기술 부채] hiltViewModel()과 remember()가 ?.let / ?: 안에서 조건부 호출됨 —
                    // Compose Rules of Hooks 위반. homeBackStackEntry는 실제로 null → non-null
                    // 단방향 전환만 하므로 현재 nav 구조에서는 안전하게 동작하나,
                    // 근본 해결을 위해서는 RecoverySessionHolder 싱글톤 도입 필요.
                    val homeBackStackEntry = remember(navBackStackEntry) {
                        try { navController.getBackStackEntry<Routes.HomeMain>() }
                        catch (_: Exception) { null }
                    }
                    val homeViewModel = homeBackStackEntry?.let { hiltViewModel<HomeViewModel>(it) }
                    val recoverySession: StudyingSession? by (
                        homeViewModel
                            ?.recoverySession
                            ?.collectAsState()
                            ?: remember { mutableStateOf(null) }
                    )

                    // 홈 화분이 화면에 반영된 뒤 스플래시를 끝내 기본 화면이 보이지 않게 한다.
                    // 화분이 없는 경우 등은 SplashViewModel이 처리한다.
                    LaunchedEffect(homeViewModel) {
                        homeViewModel?.displayPot?.first { it != Pot.EMPTY } ?: return@LaunchedEffect
                        withFrameNanos { }
                        viewModel.onFirstScreenReady()
                    }

                    val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                        destination.hasRoute<Routes.HomeMain>() ||
                                destination.hasRoute<Routes.CommunityList>() ||
                                destination.hasRoute<Routes.Mypage>() ||
                                destination.hasRoute<Routes.Report>() ||
                                destination.hasRoute<Routes.PotList>()
                    } ?: false
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) BottomBar(
                                navController = navController,
                                enabled = recoverySession == null
                            )
                        }
                    ) { innerPadding ->
                        // Modifier.padding()은 inset을 소비하지 않아, 하위 화면의 Scaffold나
                        // imePadding() 등이 같은 시스템 인셋(내비바 등)을 중복으로 더하게 됨.
                        // consumeWindowInsets로 여기서 소비해 하위에서 중복 적용되지 않도록 한다.
                        Box(
                            modifier = Modifier
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            PlantAppNavigation(navController = navController, viewModel = viewModel)
                        }
                    }
                }

            }
        }
    }
}
