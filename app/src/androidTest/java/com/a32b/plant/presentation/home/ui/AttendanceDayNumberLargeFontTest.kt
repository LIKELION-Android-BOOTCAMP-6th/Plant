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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.home.viewmodel.AttendanceUiState
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 출석판 원 안의 날짜 숫자·"✓"가 좁은 화면·큰 글꼴에서도 잘리지 않는지 확인 (#755)
 *
 * - 화면 폭·글꼴 배율을 바꾸는 방법은 [AttendanceGoldLabelLargeFontTest]와 같음 (Dialog 대신 내용물을 Box 폭으로 그림)
 * - 출석 9칸 완료 상태로 그린다: 1~9일은 "✓", 다음 칸 10일은 두 자리 굵은 글꼴(titleSmall), 나머지 숫자 칸은 bodySmall
 * - 폭: 280dp(이슈의 더 좁은 화면 예), 300dp(이슈 재현 조건), 320dp·340dp(300~360dp 사이), 360dp, 411dp(SM-N986N 폭)
 */
@RunWith(AndroidJUnit4::class)
class AttendanceDayNumberLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var measurer: TextMeasurer? = null
    private var bodySmall: TextStyle? = null
    private var titleSmall: TextStyle? = null

    @Composable
    private fun Frame(width: Dp, fontScale: Float, darkTheme: Boolean) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.requiredWidth(width)) {
                    AttendanceDialogContent(uiState = AttendanceUiState(count = CHECKED_COUNT, buttonText = "출석하기"))
                }
                measurer = rememberTextMeasurer()
                bodySmall = MaterialTheme.typography.bodySmall
                titleSmall = MaterialTheme.typography.titleSmall
            }
        }
    }

    /** 폭·글꼴 배율 조합마다 새로 그려(key) [check]를 부른다 */
    private fun forEachCombo(
        widths: List<Dp> = WIDTHS,
        fontScales: List<Float> = FONT_SCALES,
        darkTheme: Boolean = false,
        combos: List<Pair<Dp, Float>> = widths.flatMap { w -> fontScales.map { w to it } },
        check: (width: Dp, fontScale: Float) -> Unit
    ) {
        var combo by mutableStateOf(combos.first())
        composeRule.setContent {
            key(combo) { Frame(combo.first, combo.second, darkTheme) }
        }
        for ((width, fontScale) in combos) {
            composeRule.runOnIdle { combo = width to fontScale }
            composeRule.waitForIdle()
            check(width, fontScale)
        }
    }

    private fun layoutOf(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    private fun nodeOf(text: String) = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()

    /**
     * 숫자가 잘리지 않았는지: 한 줄에 모든 글자가 배치되고(수정 전에는 두 줄로 줄바꿈돼 둘째 줄이 잘림), 줄 폭이 원 폭 안에 들어감
     */
    private fun assertOneLineInCircle(label: String, text: String) {
        val r = layoutOf(nodeOf(text))
        assertEquals("$label: 줄 수", 1, r.lineCount)
        assertFalse("$label: 줄 수 제한에 걸려 잘림", r.multiParagraph.didExceedMaxLines)
        assertEquals("$label: 배치되지 않은 글자 있음", text.length, r.getLineEnd(0, visibleEnd = true))
        val circleWidth = r.layoutInput.constraints.maxWidth
        val lineWidth = r.getLineRight(0) - r.getLineLeft(0)
        assertTrue("$label: 줄 폭 ${lineWidth}px가 원 폭 ${circleWidth}px보다 넓음", lineWidth <= circleWidth)
    }

    /** 숫자 칸 전부(다음 칸 10일 포함) 한 줄로 원 안에 들어감 */
    @Test
    fun dayNumbers_neverClipped() {
        forEachCombo { width, fontScale ->
            for (text in DAY_NUMBERS) assertOneLineInCircle("폭 $width, 글꼴 $fontScale, $text", text)
        }
    }

    /**
     * "✓"가 원 가운데에 있고 기준선이 원 안에 있음
     * - 원 위치·크기: 같은 행 숫자 칸 "11"로 구함 (숫자는 줄 높이가 원보다 작아 원 가운데에 놓이고, 원 폭 = 높이)
     * - 수정 전 300dp·글꼴 2.0: "✓" 줄 높이 91px > 원 69px라 원 높이로 줄어든 채 위쪽부터 그려져 기준선(73px)이 원 밖 (임시 계측 로그)
     */
    @Test
    fun checkMark_centeredInCircle() {
        forEachCombo { width, fontScale ->
            val label = "폭 $width, 글꼴 $fontScale"
            val ref = nodeOf("11")
            val refLayout = layoutOf(ref)
            // 높이 제약은 wrapContentHeight(unbounded = true)로 풀려 있어 폭으로 구함 (원은 aspectRatio(1f))
            val circleSize = refLayout.layoutInput.constraints.maxWidth
            val circleCenterY = ref.positionInRoot.y + ref.size.height / 2f
            val circleBottom = circleCenterY + circleSize / 2f
            // 같은 행(2행)의 "✓": 8일·9일
            val checks = composeRule.onAllNodesWithText("✓", useUnmergedTree = true).fetchSemanticsNodes()
                .filter { abs(it.positionInRoot.y + it.size.height / 2f - circleCenterY) < circleSize }
            assertEquals("$label: 2행 ✓ 개수", 2, checks.size)
            for (check in checks) {
                val r = layoutOf(check)
                val centerY = check.positionInRoot.y + check.size.height / 2f
                assertEquals("$label: ✓ 세로 가운데가 원 가운데와 다름", circleCenterY, centerY, 1f)
                val baseline = check.positionInRoot.y + r.firstBaseline
                assertTrue("$label: ✓ 기준선 ${baseline}px가 원 아래 ${circleBottom}px 밖", baseline <= circleBottom)
            }
        }
    }

    /**
     * 원래 스타일(12sp) 한 줄이 원 폭에 들어가는 칸은 글자 크기가 그대로 (자동 크기 조절이 켜지지 않음)
     * - 비교 방법은 [AttendanceGoldLabelLargeFontTest.goldLabels_sameSizeAsBeforeAtDefaultFont]와 같음: 실제 줄 폭을 원래 스타일 한 줄 폭과 비교
     * - 들어가는지는 조건마다 잰다: 같은 조건이라도 칸마다 원 폭이 1px 다르고(반올림), 다음 칸(굵은 글꼴)이 더 넓다
     *   (예: 280dp·글꼴 1.8에서 원 62px, "10"은 보통 글꼴 62px로 들어가고 굵은 글꼴 64px로 안 들어감 — 이 테스트 작성 중 SM-N986N 출력)
     * - 이슈의 "지금처럼 보임" 조건(300dp·글꼴 1.8, 360dp·글꼴 1.8·2.0, 360·411dp·글꼴 1.0)에서는
     *   모든 숫자가 들어가 크기가 그대로인지도 확인한다 (들어가는 칸이 없어 검사가 비는 것을 막음)
     */
    @Test
    fun dayNumbers_sameSizeAsBefore_whenFitting() {
        val unchangedInIssue = listOf(300.dp to 1.8f, 360.dp to 1.8f, 360.dp to 2.0f, 360.dp to 1.0f, 411.dp to 1.0f)
        forEachCombo { width, fontScale ->
            for (text in DAY_NUMBERS) {
                val label = "폭 $width, 글꼴 $fontScale, $text"
                val style = if (text == NEXT_DAY) titleSmall!! else bodySmall!!
                val expected = measurer!!.measure(text, style, maxLines = 1)
                val expectedWidth = expected.getLineRight(0) - expected.getLineLeft(0)
                val r = layoutOf(nodeOf(text))
                val fits = expectedWidth <= r.layoutInput.constraints.maxWidth
                if ((width to fontScale) in unchangedInIssue) assertTrue("$label: 이슈의 정상 조건인데 원래 크기로 안 들어감", fits)
                if (!fits) continue
                assertEquals(
                    "$label: 줄 폭이 원래 크기 한 줄 폭과 다름",
                    expectedWidth,
                    r.getLineRight(0) - r.getLineLeft(0),
                    0.5f
                )
            }
        }
    }

    /** 다크 모드에서도 잘리지 않음 */
    @Test
    fun dayNumbers_neverClipped_darkTheme() {
        forEachCombo(widths = listOf(280.dp, 300.dp, 360.dp), fontScales = listOf(1.0f, 2.0f), darkTheme = true) { width, fontScale ->
            for (text in DAY_NUMBERS) assertOneLineInCircle("다크, 폭 $width, 글꼴 $fontScale, $text", text)
        }
    }

    companion object {
        const val CHECKED_COUNT = 9
        const val NEXT_DAY = "10"

        // 9칸 완료 상태에서 숫자로 보이는 칸 (아이콘·골드 칸 제외: 12·14·17·20·21·23·25·28)
        val DAY_NUMBERS = listOf("10", "11", "13", "15", "16", "18", "19", "22", "24", "26", "27")
        val WIDTHS = listOf(280.dp, 300.dp, 320.dp, 340.dp, 360.dp, 411.dp)
        val FONT_SCALES = listOf(1.0f, 1.3f, 1.5f, 1.8f, 2.0f)
    }
}
