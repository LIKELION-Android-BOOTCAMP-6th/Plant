package com.a32b.plant.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController

/**
 * 이 화면이 맨 위일 때만 뒤로 가는 함수를 돌려준다 (#672).
 *
 * - 연타: 첫 pop 뒤에는 맨 위가 이전 화면이라, 나가는 중인 이 화면에서 다시 눌러도 무시된다
 * - 화면 lifecycle을 보지 않으므로, 앱이 백그라운드일 때 ViewModel 결과(NavigateBack 이벤트)로 닫는 경우에도 쓸 수 있다
 * - 화면(NavHost 목적지) 함수의 맨 위에서 불러 쓴다. NavHost가 그 화면의 back stack entry를 LocalLifecycleOwner로 넣어 주는 것을 이용한다
 * - Dialog 창 안에서 부르지 않는다. Dialog 안의 LocalLifecycleOwner는 이 화면이 아니라 Activity여서 뒤로 가지 않는다
 *   (compose-ui 1.11.2 소스 기준). 화면 함수에서 만든 함수를 넘겨 쓸 것
 */
@Composable
fun rememberPopBackStackSafely(navController: NavController): () -> Unit {
    val screenEntry = LocalLifecycleOwner.current
    return remember(navController, screenEntry) {
        {
            if (navController.currentBackStackEntry === screenEntry) {
                navController.popBackStack()
            }
        }
    }
}
