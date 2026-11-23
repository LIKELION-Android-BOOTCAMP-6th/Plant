package com.a32b.plant.presentation.core.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.ColorUtils
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
 * - 배경: 확인 버튼 캡처의 왼쪽 가장자리 세로 가운데 픽셀(모서리 둥근 부분·글자를 피한 곳)이 기대 배경색인지
 * - 글자: 캡처 안에 글자색과 정확히 같은 픽셀이 있는지 (글자 안쪽은 안티앨리어싱 없이 글자색 그대로 칠해짐)
 * - 대비: 스킴의 error / onError 대비가 WCAG AA 일반 글자 기준(4.5:1) 이상인지
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

        val pixels = composeRule
            .onNode(hasClickAction() and hasText(confirmText), useUnmergedTree = false)
            .captureToImage()
            .toPixelMap()

        val edge = pixels[2, pixels.height / 2]
        assertEquals("확인 버튼 배경색", expectedContainer.toArgb(), edge.toArgb())

        val textArgb = expectedText.toArgb()
        var textPixels = 0
        for (x in 0 until pixels.width) for (y in 0 until pixels.height) {
            if (pixels[x, y].toArgb() == textArgb) textPixels++
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
    fun errorOnErrorContrast_meetsAA() {
        val light = ColorUtils.calculateContrast(LightColorScheme.onError.toArgb(), LightColorScheme.error.toArgb())
        val dark = ColorUtils.calculateContrast(DarkColorScheme.onError.toArgb(), DarkColorScheme.error.toArgb())
        assertTrue("라이트 대비 $light", light >= 4.5)
        assertTrue("다크 대비 $dark", dark >= 4.5)
    }
}
