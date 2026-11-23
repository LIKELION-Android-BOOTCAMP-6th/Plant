package com.a32b.plant.presentation.core.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.theme.DarkColorScheme
import com.a32b.plant.presentation.theme.LightColorScheme
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [ConfirmDialog] isDestructive 확인 버튼 색 (#647)
 *
 * - 배경: 확인 버튼 캡처의 왼쪽 가장자리(x = 2px) 세로 가운데 픽셀이 기대 배경색인지
 * - 글자: 버튼 안 Text 노드 캡처 안에 글자색과 정확히 같은 픽셀이 1개 이상 있는지.
 *   버튼 노드 캡처에는 버튼 모양 밖의 카드 배경도 찍혀, 라이트의 onError(흰색)와 카드(흰색)를 구분하지 못한다
 * - error / onError 대비(4.5:1 이상)는 PlantColorSchemeTest(단위 테스트)에서 확인
 * - 로딩: isDestructive와 isLoading을 함께 켜도 로딩 표시가 뜨고, 확인 버튼·뒤로가기가 막히는지
 */
@RunWith(AndroidJUnit4::class)
class ConfirmDialogDestructiveTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun assertConfirmButtonColors(
        darkTheme: Boolean,
        isDestructive: Boolean,
        confirmText: String,
        expectedContainer: Color,
        expectedText: Color
    ) {
        composeRule.setContent {
            PlantTheme(darkTheme = darkTheme) {
                ConfirmDialog(
                    text = "확인",
                    onDismiss = {},
                    onConfirm = {},
                    confirmBtnText = confirmText,
                    isDestructive = isDestructive
                )
            }
        }
        composeRule.waitForIdle()

        val buttonPixels = composeRule
            .onNode(hasClickAction() and hasText(confirmText))
            .captureToImage()
            .toPixelMap()

        val edge = buttonPixels[2, buttonPixels.height / 2]
        assertEquals("확인 버튼 배경색", expectedContainer.toArgb(), edge.toArgb())

        val textPixelMap = composeRule
            .onNode(hasText(confirmText), useUnmergedTree = true)
            .captureToImage()
            .toPixelMap()
        val textArgb = expectedText.toArgb()
        var textPixels = 0
        for (x in 0 until textPixelMap.width) for (y in 0 until textPixelMap.height) {
            if (textPixelMap[x, y].toArgb() == textArgb) textPixels++
        }
        assertTrue("확인 버튼 글자색 픽셀 없음", textPixels > 0)
    }

    @Test
    fun destructiveLight_usesErrorAndOnError() = assertConfirmButtonColors(
        darkTheme = false, isDestructive = true, confirmText = "삭제",
        expectedContainer = LightColorScheme.error, expectedText = LightColorScheme.onError
    )

    @Test
    fun destructiveDark_usesErrorAndOnError() = assertConfirmButtonColors(
        darkTheme = true, isDestructive = true, confirmText = "탈퇴",
        expectedContainer = DarkColorScheme.error, expectedText = DarkColorScheme.onError
    )

    // 기본값(isDestructive = false)은 바뀌기 전과 같이 primary 배경 + Type.kt 스타일 글자색(onSurface)
    @Test
    fun defaultLight_unchanged() = assertConfirmButtonColors(
        darkTheme = false, isDestructive = false, confirmText = "예",
        expectedContainer = LightColorScheme.primary, expectedText = LightColorScheme.onSurface
    )

    @Test
    fun defaultDark_unchanged() = assertConfirmButtonColors(
        darkTheme = true, isDestructive = false, confirmText = "예",
        expectedContainer = DarkColorScheme.primary, expectedText = DarkColorScheme.onSurface
    )

    @Test
    fun destructiveLoading_showsProgressAndBlocksConfirmAndBack() {
        var confirmCalls = 0
        var dismissCalls = 0
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                ConfirmDialog(
                    text = "기록 삭제",
                    onDismiss = { dismissCalls++ },
                    onConfirm = { confirmCalls++ },
                    confirmBtnText = "삭제",
                    isDestructive = true,
                    isLoading = true
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)

        composeRule.onNode(hasClickAction() and hasText("삭제")).performClick()
        composeRule.waitForIdle()
        assertEquals("로딩 중 확인 버튼이 눌림", 0, confirmCalls)

        Espresso.pressBackUnconditionally()
        composeRule.waitForIdle()
        assertEquals("로딩 중 뒤로가기로 닫힘", 0, dismissCalls)
    }
}
