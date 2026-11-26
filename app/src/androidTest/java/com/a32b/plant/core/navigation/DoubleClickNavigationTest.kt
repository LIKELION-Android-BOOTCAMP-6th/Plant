package com.a32b.plant.core.navigation

import androidx.activity.BackEventCompat
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessStarted
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 버튼 연타 시 화면 중복 이동·두 번 뒤로가기 방지 (#672)
 *
 * 가로 slide 전환(TRANSITION_MS)을 둔 NavHost에서, mainClock.autoAdvance = false로 시간을 멈추고 같은 버튼을 두 번 누른다.
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
                        TextButton(onClick = dropUnlessStarted { navController.navigate(B) }) { Text("safe-to-b") }
                        TextButton(onClick = dropUnlessStarted { navController.navigate(C) }) { Text("safe-to-c") }
                        TextButton(onClick = { navController.navigate(B) }) { Text("plain-to-b") }
                        TextButton(onClick = { navController.navigate(C) }) { Text("to-c") }
                    }
                }
                composable(C) {
                    TextButton(onClick = { navController.navigate(B) }) { Text("c-to-b") }
                }
                composable(B) {
                    val popBackStackSafely = rememberPopBackStackSafely(navController)
                    Column {
                        TextButton(onClick = popBackStackSafely) { Text("safe-back") }
                        TextButton(onClick = { navController.popBackStack() }) { Text("plain-back") }
                        TextButton(onClick = dropUnlessStarted { navController.navigate(C) }) { Text("b-to-c") }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** 아래부터 쌓인 순서 (그래프 항목 제외) */
    private fun routes(): List<String?> {
        var result: List<String?> = emptyList()
        composeRule.runOnUiThread {
            result = navController.currentBackStack.value.filter { it.destination !is NavGraph }.map { it.destination.route }
        }
        return result
    }

    private fun topState(): Lifecycle.State {
        var state: Lifecycle.State? = null
        composeRule.runOnUiThread { state = navController.currentBackStackEntry?.lifecycle?.currentState }
        return state!!
    }

    /** A → C → B 로 쌓고 전환이 모두 끝날 때까지 기다린다 */
    private fun openBOverC() {
        composeRule.onNodeWithText("to-c").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("c-to-b").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A, C, B), routes())
    }

    /**
     * 예측 뒤로가기를 시작했다가 진행 이벤트 없이 취소한다. NavHost가 맨 위 화면을 STARTED로 둔 채 끝나는 경우 (navigation-compose 2.9.8)
     * 실제 제스처처럼 시작과 취소를 따로 보내고 그 사이 메인 스레드가 한 번 돌게 한다 (시작 때 NavHost가 띄우는 코루틴이 먼저 실행되도록)
     */
    private fun startAndCancelPredictiveBack() {
        val dispatcher = composeRule.activity.onBackPressedDispatcher
        composeRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(BackEventCompat(10f, 500f, 0f, BackEventCompat.EDGE_LEFT))
        }
        composeRule.waitForIdle()
        composeRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        composeRule.waitForIdle()
    }

    @Test
    fun dropUnlessStarted_doubleClick_navigatesOnce() {
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
    fun dropUnlessStarted_otherButtonDuringExitTransition_ignored() {
        setNavHost()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("safe-to-b").performClick()
        composeRule.mainClock.advanceTimeBy(100)   // 나가는 전환 중
        composeRule.onNodeWithText("safe-to-c").performClick()

        assertEquals(listOf(A, B), routes())
    }

    @Test
    fun plainNavigate_otherButtonDuringExitTransition_navigates() {
        // 위 테스트의 대조군: 같은 시점(100ms 뒤)의 클릭이 나가는 화면의 버튼에 실제로 닿는지 확인한다
        setNavHost()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("safe-to-b").performClick()
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithText("to-c").performClick()

        assertEquals(listOf(A, B, C), routes())
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
    fun popBackStackSafely_secondClickDuringExitTransition_ignored() {
        setNavHost()
        openBOverC()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.mainClock.advanceTimeBy(150)   // 나가는 전환 중 (화면이 아직 그려져 있음)
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
        // 한 번 뒤로 간 뒤에도 막힌 채로 남지 않는다: 전환이 끝난 뒤 다시 들어온 화면에서 누르면 정상적으로 뒤로 간다
        setNavHost()
        openBOverC()

        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A, C), routes())
        composeRule.runOnUiThread { navController.navigate(B) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(A, C), routes())
    }

    @Test
    fun predictiveBackCancelledWithoutProgress_buttonsStillWork() {
        // 맨 위 화면이 STARTED에 머물러도 이동·뒤로가기 버튼이 반응해야 한다 (RESUMED 기준이면 둘 다 무시됨)
        setNavHost()
        composeRule.onNodeWithText("safe-to-b").performClick()
        composeRule.waitForIdle()
        startAndCancelPredictiveBack()
        // STARTED가 재현되지 않으면(라이브러리 변경 등) 확인할 상황이 없으므로 건너뛴다 — 건너뛴 테스트 수를 함께 볼 것
        assumeTrue(topState() == Lifecycle.State.STARTED)

        composeRule.onNodeWithText("b-to-c").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A, B, C), routes())
    }

    @Test
    fun predictiveBackCancelledWithoutProgress_backStillWorks() {
        setNavHost()
        composeRule.onNodeWithText("safe-to-b").performClick()
        composeRule.waitForIdle()
        startAndCancelPredictiveBack()
        // STARTED가 재현되지 않으면(라이브러리 변경 등) 확인할 상황이 없으므로 건너뛴다 — 건너뛴 테스트 수를 함께 볼 것
        assumeTrue(topState() == Lifecycle.State.STARTED)

        composeRule.onNodeWithText("safe-back").performClick()
        composeRule.waitForIdle()
        assertEquals(listOf(A), routes())
    }

    private companion object {
        const val A = "a"
        const val B = "b"
        const val C = "c"
        const val TRANSITION_MS = 400
    }
}
