package com.a32b.plant.presentation.core.component

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a32b.plant.presentation.theme.DarkColorScheme
import com.a32b.plant.presentation.theme.LightColorScheme
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 전체 화면 로딩 창([LoadingDialog])과 다이얼로그 안 로딩([ConfirmDialog] isLoading) 확인 (#648)
 *
 * - 크기: Dialog 내용(LoadingBox)이 액티비티 창(decorView)과 같은 크기인지 (상태바·내비게이션 바 포함)
 * - 색: 기기 화면 캡처(UiAutomation)로 실제 합성된 색을 본다. Dialog 창 dim은 앱 화면 캡처(captureToImage)에 안 잡히므로
 *   "배경색 위에 scrim 32% 한 겹"이면 dim이 꺼진 것이다
 */
@RunWith(AndroidJUnit4::class)
class LoadingDialogTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var isLoading by mutableStateOf(true)

    private fun showLoadingDialog(darkTheme: Boolean) {
        composeRule.setContent {
            PlantTheme(darkTheme = darkTheme) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    if (isLoading) LoadingDialog()
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun dialogCoversWholeWindow() {
        showLoadingDialog(darkTheme = false)

        val dialogRoot = composeRule.onNode(isDialog()).fetchSemanticsNode()
        val decorView = composeRule.activity.window.decorView
        assertEquals("폭", decorView.width, dialogRoot.size.width)
        assertEquals("높이", decorView.height, dialogRoot.size.height)
    }

    @Test
    fun backPressDoesNotCloseDialogOrActivity() {
        showLoadingDialog(darkTheme = false)

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
        assertFalse("액티비티가 종료됨", composeRule.activity.isFinishing)
    }

    @Test
    fun dialogDisappearsWhenLoadingEnds() {
        showLoadingDialog(darkTheme = false)
        composeRule.onAllNodes(isDialog()).assertCountEquals(1)

        composeRule.runOnIdle { isLoading = false }
        composeRule.waitForIdle()

        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test
    fun lightThemeDimsBackgroundOnceWithScrim() {
        showLoadingDialog(darkTheme = false)
        assertScreenColorEventually(expectedOverlay(LightColorScheme.background.toArgb(), LightColorScheme.scrim.toArgb()))
    }

    @Test
    fun darkThemeDimsBackgroundOnceWithScrim() {
        showLoadingDialog(darkTheme = true)
        assertScreenColorEventually(expectedOverlay(DarkColorScheme.background.toArgb(), DarkColorScheme.scrim.toArgb()))
    }

    @Test
    fun confirmDialogLoadingStaysInsideDialogAndBlocksBack() {
        var dismissCalls = 0
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                ConfirmDialog(
                    text = "탈퇴할까요?",
                    onDismiss = { dismissCalls++ },
                    onConfirm = {},
                    isLoading = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
        val dialogRoot = composeRule.onNode(isDialog()).fetchSemanticsNode()
        assertTrue("다이얼로그가 화면 전체로 커짐", dialogRoot.size.width < composeRule.activity.window.decorView.width)

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()
        assertEquals("로딩 중 뒤로가기로 닫힘", 0, dismissCalls)
    }

    /** 배경색 위에 scrim을 [LOADING_SCRIM_ALPHA]로 한 번 덮은 색 */
    private fun expectedOverlay(background: Int, scrim: Int): Int {
        fun blend(bg: Int, fg: Int) = (fg * LOADING_SCRIM_ALPHA + bg * (1 - LOADING_SCRIM_ALPHA)).roundToInt()
        return AndroidColor.rgb(
            blend(AndroidColor.red(background), AndroidColor.red(scrim)),
            blend(AndroidColor.green(background), AndroidColor.green(scrim)),
            blend(AndroidColor.blue(background), AndroidColor.blue(scrim))
        )
    }

    /**
     * 화면 가운데 아래쪽(스피너와 겹치지 않는 곳)의 색을 기기 캡처로 확인한다.
     * Dialog 창이 뜨는 애니메이션이 끝날 때까지 최대 3초 다시 캡처한다.
     */
    private fun assertScreenColorEventually(expected: Int) {
        val deadline = SystemClock.uptimeMillis() + 3_000
        var actual: Int
        do {
            val screen: Bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            actual = screen.getPixel(screen.width / 2, screen.height * 3 / 4)
            if (sameColor(expected, actual)) return
            SystemClock.sleep(200)
        } while (SystemClock.uptimeMillis() < deadline)
        throw AssertionError("기대 ${hex(expected)}, 실제 ${hex(actual)}")
    }

    // 합성·반올림 차이 허용 (채널당 2)
    private fun sameColor(a: Int, b: Int) =
        abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 2 &&
            abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 2 &&
            abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 2

    private fun hex(color: Int) = String.format("#%06X", color and 0xFFFFFF)
}
