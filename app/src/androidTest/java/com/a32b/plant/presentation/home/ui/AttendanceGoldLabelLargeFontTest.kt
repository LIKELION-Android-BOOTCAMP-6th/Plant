package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.home.viewmodel.AttendanceUiState
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 출석판 골드 보상 글자("100G"~"500G")가 큰 글꼴에서도 잘리지 않는지 확인 (#741)
 *
 * - 화면 폭은 Box 폭으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다
 *   (Dialog 안에는 이 덮어쓰기가 닿지 않아 다이얼로그 내용물 AttendanceDialogContent를 직접 그린다.
 *   내용물은 fillMaxWidth(0.92f)라 Box 폭이 화면 폭 역할을 한다)
 * - 폭: 320dp(#565·#572 확인 조건), 360dp(이슈의 360dp), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
 *   기기 폭보다 넓은 값도 그대로 쓰도록 requiredWidth로 정한다
 */
@RunWith(AndroidJUnit4::class)
class AttendanceGoldLabelLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun Frame(width: Dp, fontScale: Float, darkTheme: Boolean = false, extra: @Composable () -> Unit = {}) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.requiredWidth(width)) {
                    AttendanceDialogContent(uiState = AttendanceUiState(count = 8, buttonText = "출석하기"))
                }
                extra()
            }
        }
    }

    /** 폭·글꼴 배율 조합마다 새로 그려(key) [check]를 부른다 */
    private fun forEachCombo(
        widths: List<Dp> = WIDTHS,
        fontScales: List<Float> = FONT_SCALES,
        darkTheme: Boolean = false,
        check: (width: Dp, fontScale: Float) -> Unit
    ) {
        var combo by mutableStateOf(widths.first() to fontScales.first())
        composeRule.setContent {
            key(combo) { Frame(combo.first, combo.second, darkTheme) }
        }
        for (width in widths) for (fontScale in fontScales) {
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

    /**
     * 글자가 잘리지 않았는지: 모든 글자가 줄에 배치되고(maxLines에 걸려 잘린 글자 없음), 글자 전체 높이가 Text 높이 안에 들어감
     * - 골드 글자 Text는 softWrap 기본값(true)이라 가로로 넘치는 글자는 줄바꿈되고 maxLines = 1에 걸리므로 위 검사로 잡힌다
     *   (수정 전 "G"가 안 보이던 것도 이 경로: 줄바꿈된 "G"가 1줄 제한으로 잘림)
     */
    private fun assertNotClipped(label: String, text: String) {
        val r = layoutOf(text)
        assertFalse("$label: 줄 수 제한에 걸려 잘림", r.multiParagraph.didExceedMaxLines)
        assertEquals("$label: 배치되지 않은 글자 있음", text.length, r.getLineEnd(r.lineCount - 1, visibleEnd = true))
        assertFalse("$label: 높이 넘침 ${r.multiParagraph.height}/${r.size.height}px", r.didOverflowHeight)
    }

    /** 골드 글자 4개 모두 잘리지 않음 (이슈 케이스: 411dp·360dp 글꼴 1.0~2.0, 320dp 글꼴 1.3 + 이슈 표 밖인 320dp 글꼴 1.5~2.0) */
    @Test
    fun goldLabels_neverClipped() {
        forEachCombo { width, fontScale ->
            for (text in GOLD_LABELS) {
                assertNotClipped("폭 $width, 글꼴 $fontScale, $text", text)
            }
        }
    }

    /**
     * 글꼴 1.0(320·360·411dp)에서는 수정 전처럼 labelSmall(11sp) 그대로 (자동 크기 조절이 켜지지 않음)
     * - semantics로 받은 layoutInput.style은 autoSize로 고른 크기가 아니라 Text에 넘긴 style이라 글자 크기 비교로는 못 잡음
     *   (foundation 1.11.2 TextAnnotatedStringNode.applySemantics) → 실제 줄 폭을 11sp 한 줄 폭과 비교한다
     * - 한 단계(0.25sp) 작으면 "100G" 폭이 약 1.5px 줄어든다 (11sp 67px × 0.25/11, SM-N986N 글꼴 1.0 측정값으로 계산) → 0.5px 오차 안에 들지 않음
     * - 한계: SM-N986N(420dpi)에서는 autoSize를 항상 켜도 11sp로 나와 이 테스트가 통과한다 (임시로 항상 켜서 확인함).
     *   한 단계 작게 나오는 dpi(PR #734: 448·453dpi)를 막는 것은 코드의 "들어가면 autoSize를 켜지 않음" 분기이고,
     *   이 테스트는 그 분기가 수정 전 크기를 바꾸지 않는지 확인한다
     */
    @Test
    fun goldLabels_sameSizeAsBeforeAtDefaultFont() {
        val widths = listOf(320.dp, 360.dp, 411.dp)
        var width by mutableStateOf(widths.first())
        var measurer: TextMeasurer? = null
        var style: TextStyle? = null
        composeRule.setContent {
            key(width) {
                Frame(width, 1.0f) {
                    measurer = rememberTextMeasurer()
                    style = MaterialTheme.typography.labelSmall
                }
            }
        }
        for (w in widths) {
            composeRule.runOnIdle { width = w }
            composeRule.waitForIdle()
            for (text in GOLD_LABELS) {
                val r = layoutOf(text)
                // 같은 폭·가운데 정렬로 재야 줄 위치의 px 맞춤까지 같아진다 (제약 없이 재면 320dp에서 67px, 실제 68px로 1px 차이)
                val expected = measurer!!.measure(
                    text,
                    style!!.copy(textAlign = TextAlign.Center),
                    maxLines = 1,
                    constraints = Constraints.fixedWidth(r.layoutInput.constraints.maxWidth)
                )
                assertEquals("폭 $w, $text: 줄 수", 1, r.lineCount)
                assertEquals(
                    "폭 $w, $text: 줄 폭이 11sp 한 줄 폭과 다름",
                    expected.getLineRight(0) - expected.getLineLeft(0),
                    r.getLineRight(0) - r.getLineLeft(0),
                    0.5f
                )
            }
        }
    }

    /**
     * 골드 글자 칸이 7칸 배치·행 간격을 바꾸지 않음
     * - 폭: 같은 행 아이템 칸의 공백 글자와 같음 (칸 폭을 정하는 width(42.dp)를 그대로 씀)
     * - 위치: 같은 행 공백 글자와 위쪽 끝이 같음 (원 크기·원 아래 간격이 같음)
     * - 높이: 공백 글자보다 크지 않음 → 행 높이는 그 행의 공백 글자들이 정하므로 다음 행 위치가 바뀌지 않음
     *   (글자를 줄이면 줄 높이도 조금 줄어든다: 320dp·글꼴 1.3에서 공백 38px, 골드 36px — 이 테스트 작성 중 SM-N986N 출력)
     * - 위치·크기는 잘리지 않은 값(positionInRoot·size, px)으로 비교 — boundsInRoot는 스크롤 영역에 잘린 값이라 쓰지 않음
     */
    @Test
    fun goldLabels_keepBoardLayout() {
        forEachCombo { width, fontScale ->
            val blanks = composeRule.onAllNodesWithText(" ", useUnmergedTree = true).fetchSemanticsNodes()
            // 28칸 중 골드 4칸을 뺀 24칸이 공백 글자
            assertEquals("폭 $width, 글꼴 $fontScale: 공백 글자 수", 24, blanks.size)
            for (text in GOLD_LABELS) {
                val label = "폭 $width, 글꼴 $fontScale, $text"
                val gold = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
                val sameRowBlank = blanks.firstOrNull { abs(it.positionInRoot.y - gold.positionInRoot.y) < 1f }
                assertNotNull("$label: 위쪽 끝이 같은 공백 글자 없음 (골드 y=${gold.positionInRoot.y}px)", sameRowBlank)
                assertEquals("$label: 폭(px)이 공백 글자와 다름", sameRowBlank!!.size.width, gold.size.width)
                assertTrue(
                    "$label: 높이 ${gold.size.height}px가 공백 글자 ${sameRowBlank.size.height}px보다 큼",
                    gold.size.height <= sameRowBlank.size.height
                )
            }
        }
    }

    /** 다크 모드에서도 잘리지 않음 (색만 바뀌고 배치는 같아야 함) */
    @Test
    fun goldLabels_neverClipped_darkTheme() {
        forEachCombo(widths = listOf(360.dp, 411.dp), fontScales = listOf(1.0f, 2.0f), darkTheme = true) { width, fontScale ->
            for (text in GOLD_LABELS) assertNotClipped("다크, 폭 $width, 글꼴 $fontScale, $text", text)
        }
    }

    companion object {
        val GOLD_LABELS = listOf("100G", "200G", "300G", "500G")
        val WIDTHS = listOf(320.dp, 360.dp, 411.dp)
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
    }
}
