package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 큰 글꼴에서 하단바 탭 이름이 글자 중간에서 줄바꿈되지 않는지 확인 (#736)
 *
 * - 화면 폭은 Box 폭(requiredWidth)으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다 (SmallScreenLargeFontTest와 같은 방식)
 * - 탭 선택 상태는 그리지 않는다 (NavHost 없이 그려 선택된 탭 없음). 선택 여부는 색만 바꾸고 크기에는 영향 없음
 */
@RunWith(AndroidJUnit4::class)
class BottomBarLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun forEachCombo(darkTheme: Boolean = false, check: (width: Dp, fontScale: Float) -> Unit) {
        var combo by mutableStateOf(WIDTHS.first() to FONT_SCALES.first())
        composeRule.setContent {
            key(combo) { Frame(combo.first, combo.second, darkTheme) }
        }
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            composeRule.runOnIdle { combo = width to fontScale }
            composeRule.waitForIdle()
            check(width, fontScale)
        }
    }

    @Composable
    private fun Frame(width: Dp, fontScale: Float, darkTheme: Boolean) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.requiredWidth(width)) { BottomBar(rememberNavController()) }
            }
        }
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    /** 다섯 탭 이름: 한 줄, 말줄임 없음(다 들어감), 같은 글자 크기, 같은 위·아래 위치(아이콘 높이 같음) */
    private fun assertLabelsOneLineAligned(label: String) {
        val layouts = BOTTOM_BAR_LABELS.associateWith { layoutOf(it) }
        layouts.forEach { (text, r) ->
            assertEquals("$label $text: 줄 수", 1, r.lineCount)
            assertFalse("$label $text: 말줄임됨", r.isLineEllipsized(0))
            assertEquals("$label $text: 배치되지 않은 글자 있음", text.length, r.getLineEnd(0, visibleEnd = true))
            assertFalse("$label $text: 높이 넘침", r.didOverflowHeight)
        }
        val sizes = layouts.values.map { it.layoutInput.style.fontSize }.distinct()
        assertEquals("$label: 탭마다 글자 크기가 다름 $sizes", 1, sizes.size)
        val bounds = BOTTOM_BAR_LABELS.map {
            composeRule.onNodeWithText(it, useUnmergedTree = true).getUnclippedBoundsInRoot()
        }
        // 탭 이름 위치가 같으면 그 위의 아이콘 위치도 같음 (탭마다 세로 여백 12dp + 아이콘 21dp + 간격 5dp로 같음)
        bounds.forEach {
            assertEquals("$label: 탭 이름 위쪽 위치", bounds.first().top.value, it.top.value, TOLERANCE)
            assertEquals("$label: 탭 이름 아래쪽 위치", bounds.first().bottom.value, it.bottom.value, TOLERANCE)
        }
    }

    @Test
    fun labelsStayOneLine() {
        forEachCombo { width, fontScale -> assertLabelsOneLineAligned("폭 $width, 글꼴 $fontScale") }
    }

    @Test
    fun labelsStayOneLine_darkTheme() {
        forEachCombo(darkTheme = true) { width, fontScale -> assertLabelsOneLineAligned("다크, 폭 $width, 글꼴 $fontScale") }
    }

    /** 글꼴 1.0에서는 수정 전과 같은 bodySmall 12sp (줄어들지 않음) */
    @Test
    fun defaultFontKeepsOriginalSize() {
        forEachCombo { width, fontScale ->
            if (fontScale != 1.0f) return@forEachCombo
            BOTTOM_BAR_LABELS.forEach {
                assertEquals("폭 $width $it: 글자 크기", 12.sp, layoutOf(it).layoutInput.style.fontSize)
            }
        }
    }

    private companion object {
        // 360dp(이슈의 360×640), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
        val WIDTHS = listOf(360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        // 위치 값을 px로 반올림할 때 생기는 오차 (0.5px 이하라 0.5dp 이하)
        const val TOLERANCE = 0.5f
    }
}
