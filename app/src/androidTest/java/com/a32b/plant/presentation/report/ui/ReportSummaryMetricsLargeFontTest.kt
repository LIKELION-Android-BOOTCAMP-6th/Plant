package com.a32b.plant.presentation.report.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 리포트 상단 요약 칩: 큰 글꼴에서 긴 총 시간이 잘리지 않고, 세 칩 높이가 같음 (#649)
 *
 * - 폭은 리포트 본문 폭(화면 폭 - 좌우 여백 16dp씩): 328dp(360dp 화면), 379dp(411dp 화면 = SM-N986N)
 * - 글꼴 배율은 LocalDensity의 fontScale로 바꾼다 (SmallScreenLargeFontTest와 같은 방식)
 */
@RunWith(AndroidJUnit4::class)
class ReportSummaryMetricsLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** 폭·글꼴 배율 조합마다 [content]를 새로 그려(key) [check]를 부른다 */
    private fun forEachCombo(content: @Composable () -> Unit, check: (width: Dp, fontScale: Float) -> Unit) {
        var combo by mutableStateOf(WIDTHS.first() to FONT_SCALES.first())
        composeRule.setContent {
            key(combo) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, combo.second)) {
                    PlantTheme(darkTheme = false) {
                        Box(modifier = Modifier.requiredWidth(combo.first)) { content() }
                    }
                }
            }
        }
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            composeRule.runOnIdle { combo = width to fontScale }
            composeRule.waitForIdle()
            check(width, fontScale)
        }
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    /** 줄 수 제한에 걸린 글자가 없고 모든 글자가 배치됨 */
    private fun assertNotClipped(label: String, text: String) {
        val r = layoutOf(text)
        assertFalse("$label: 줄 수 제한에 걸려 잘림", r.multiParagraph.didExceedMaxLines)
        assertEquals("$label: 배치되지 않은 글자 있음", text.length, r.getLineEnd(r.lineCount - 1, visibleEnd = true))
    }

    /** 하루 최대에 가까운 값과 한 달 최대에 가까운 값: 어느 조합에서도 잘리지 않음, 글꼴 1.0에서는 한 줄 */
    @Test
    fun longTotalTime_neverClipped() {
        forEachCombo(content = {
            Column {
                ReportSummaryMetrics(totalMillis = DAY_MAX_MS, studyCount = 99, streakDays = 30, streakLabel = "연속")
                ReportSummaryMetrics(totalMillis = MONTH_MAX_MS, studyCount = 999, streakDays = 31, streakLabel = "최장 연속")
            }
        }) { width, fontScale ->
            val label = "폭 $width, 글꼴 $fontScale"
            for (text in listOf(DAY_MAX_TEXT, MONTH_MAX_TEXT)) {
                // 화면 글자는 단어 안에 WORD JOINER가 들어간 문자열
                val shown = keepWordsTogether(text)
                assertNotClipped("$label [$text]", shown)
                val r = layoutOf(shown)
                if (fontScale == 1.0f) assertEquals("$label [$text]: 줄 수", 1, r.lineCount)
                // 두 줄이면 시간과 분 사이(띄어쓰기)에서 나뉘어야 한다 — 숫자·단위 중간에서 쪼개지지 않음
                if (r.lineCount == 2) {
                    assertEquals("$label [$text]: 첫 줄", shown.substringBefore(' ') + " ", shown.substring(0, r.getLineEnd(0)))
                }
            }
        }
    }

    /** 총 시간이 두 줄이 되어도 세 칩 높이가 같음 */
    @Test
    fun chips_sameHeight() {
        forEachCombo(content = {
            ReportSummaryMetrics(totalMillis = DAY_MAX_MS, studyCount = 99, streakDays = 30, streakLabel = "연속")
        }) { width, fontScale ->
            val heights = listOf("총 시간, $DAY_MAX_TEXT", "학습 횟수, 99회", "연속, 30일").map {
                val b = composeRule.onNodeWithContentDescription(it).getUnclippedBoundsInRoot()
                (b.bottom - b.top).value
            }
            heights.drop(1).forEach {
                assertEquals("폭 $width, 글꼴 $fontScale: 칩 높이 $heights", heights.first(), it, TOLERANCE)
            }
        }
    }

    private companion object {
        const val DAY_MAX_MS = (23 * 60 + 59) * 60_000L
        const val DAY_MAX_TEXT = "23시간 59분"
        const val MONTH_MAX_MS = (743 * 60 + 59) * 60_000L
        const val MONTH_MAX_TEXT = "743시간 59분"
        val WIDTHS = listOf(328.dp, 379.dp)
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        // 크기 값을 px로 반올림할 때 생기는 오차
        const val TOLERANCE = 0.5f
    }
}
