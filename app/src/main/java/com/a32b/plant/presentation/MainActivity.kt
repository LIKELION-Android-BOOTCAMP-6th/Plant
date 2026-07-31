package com.a32b.plant.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.core.navigation.PlantAppNavigation
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.session.SessionExpiredObserver
import com.a32b.plant.presentation.core.component.BottomBar
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.splash.SplashViewModel
import com.a32b.plant.presentation.theme.PlantTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: SplashViewModel by viewModels()

    @Inject
    lateinit var sessionExpiredObserver: SessionExpiredObserver

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        Log.d("plantLog", "-----MainActivity")
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            //해당 값이 트루일 동안 스플래시 유지
            viewModel.destination.value == null
        }

        super.onCreate(savedInstanceState)

        setContent {
            // 다크모드 관리용
            // 원하는 페이지에 MaterialTheme.colorScheme.색상 입력한 뒤 화면 이동 -> 마이페이지 다크모드 ON OFF -> 화면 재확인 확인 가능
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            PlantTheme(darkTheme = isDarkMode) { // isDarkMode / 비활성화 = false
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
                    var isSessionDialogShown by rememberSaveable { mutableStateOf(false) }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    LaunchedEffect(Unit) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            sessionExpiredObserver.event.collect {
                                authRepository.signOut()

                                navController.navigate(Routes.SignIn) {
                                    popUpTo(0) { inclusive = true }
                                }

                                isSessionDialogShown = true
                            }
                        }


                    }

                    if (isSessionDialogShown) {
                        ConfirmDialog(
                            text = "로그인한 유저의 정보를 찾을 수 없습니다.",
                            semiText = "다시 로그인해 주세요.",
                            onDismiss = {},
                            onConfirm = { isSessionDialogShown = false }
                        )
                    }

                    val showBottomBar = navBackStackEntry?.destination?.let { destination ->
                        destination.hasRoute<Routes.HomeMain>() ||
                                destination.hasRoute<Routes.CommunityList>() ||
                                destination.hasRoute<Routes.Mypage>() || destination.hasRoute<Routes.Report>() || destination.hasRoute<Routes.PotList>()
                    } ?: false
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) BottomBar(navController = navController)
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            PlantAppNavigation(navController = navController, viewModel = viewModel)
                        }
                    }
                }

            }
        }
    }

}
