package com.a32b.plant.core.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 버튼 연타 시 화면 중복 이동·두 번 뒤로가기 방지 (#672)
 *
 * 앱 NavHost처럼 가로 slide 전환(300ms)을 둔 NavHost에서, 다음 프레임이 그려지기 전에 같은 버튼을 두 번 누르는 상황을 만든다
 * (mainClock.autoAdvance = false — 두 클릭 사이에 화면이 바뀌지 않는다).
 * 대조군(일반 navigate·popBackStack)은 같은 조건에서 두 번 일어나는 것을 함께 확인해, 테스트가 연타를 실제로 재현하는지 보인다.
 */
@RunWith(AndroidJUnit4::class)
class DoubleClickNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    private fun setNavHost() {
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = A,
                enterTransition = { slideInHorizontally(tween(TRANSITION_MS)) { it } },
                exitTransition = { slideOutHorizontally(tween(TRANSITION_MS)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(TRANSITION_MS)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(TRANSITION_MS)) { it } }
            ) {
                composable(A) {
                    Column {
                        TextButton(onClick = dropUnlessResumed { navController.navigate(B) }) { Text("safe-to-b") }
                        TextButton(onClick = { navController.navigate(B) }) { Text("plain-to-b") }
                        TextButton(onClick = { navController.navigate(C) }) { Text("to-c") }
                    }
                }
                composable(C) {
                    TextButton(onClick = { navController.navigate(B) }) { Text("c-to-b") }
                }
                composable(B) {
                    Column {
                        TextButton(onClick = { navController.popBackStackSafely() }) { Text("safe-back") }
                        TextButton(onClick = { navController.popBackStack() }) { Text("plain-back") }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** 맨 위부터가 아니라 아래부터 쌓인 순서 (그래프 항목 제외) */
    private fun routes(): List<String?> =
        navController.currentBackStack.value.filter { it.destination !is NavGraph }.map { it.destination.route }

    /** A → C → B 로 쌓고 전환이 모두 끝날 때까지 기다린다 */
    private fun openBOverC() {
        composeRule.onNodeWithText("to-c").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("c-to-b").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A, C, B), routes())
    }

    @Test
    fun dropUnlessResumed_doubleClick_navigatesOnce() {
        setNavHost()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("safe-to-b").performClick()
        composeRule.onNodeWithText("safe-to-b").performClick()

        assertEquals(listOf(A, B), routes())
    }

    @Test
    fun plainNavigate_doubleClick_navigatesTwice() {
        setNavHost()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("plain-to-b").performClick()
        composeRule.onNodeWithText("plain-to-b").performClick()

        assertEquals(listOf(A, B, B), routes())
    }

    @Test
    fun popBackStackSafely_doubleClick_popsOnce() {
        setNavHost()
        openBOverC()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.onNodeWithText("safe-back").performClick()

        assertEquals(listOf(A, C), routes())
    }

    @Test
    fun plainPopBackStack_doubleClick_popsTwice() {
        setNavHost()
        openBOverC()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("plain-back").performClick()
        composeRule.onNodeWithText("plain-back").performClick()

        assertEquals(listOf(A), routes())
    }

    @Test
    fun popBackStackSafely_afterTransitionEnds_popsAgain() {
        // 연타가 아니라 전환이 끝난 뒤 다시 누르면 정상적으로 한 번 더 뒤로 간다 (항상 막는 것이 아님)
        setNavHost()
        openBOverC()

        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A, C), routes())
        var popped = false
        composeRule.runOnUiThread { popped = navController.popBackStackSafely() }
        assertEquals(true, popped)
        composeRule.waitForIdle()

        assertEquals(listOf(A), routes())
    }

    private companion object {
        const val A = "a"
        const val B = "b"
        const val C = "c"
        const val TRANSITION_MS = 300
    }
}
