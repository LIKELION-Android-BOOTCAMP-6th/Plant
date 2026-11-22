package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.presentation.mypage.ui.ButtonTemplate
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 작은 화면·큰 글꼴에서 학습·결과·마이페이지 요소가 잘리지 않는지 확인 (#673)
 *
 * - 화면 폭은 Box 폭으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다
 *   (Density(density, fontScale)도 기기 설정과 같은 비선형 배율을 쓴다 — ui-unit FontScaling.android.kt)
 * - 폭: 360dp(이슈의 360×640), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
 * - 글꼴 1.0에서는 수정 전과 같은 크기인지도 확인한다
 * - 학습 화면 전체(스크롤·사용자 카드 위치)와 프로필 다이얼로그는 ViewModel이 필요해 실기기에서 확인함
 */
@RunWith(AndroidJUnit4::class)
class SmallScreenLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Composable
    private fun Frame(width: Dp, fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = false) {
                Box(modifier = Modifier.width(width)) { content() }
            }
        }
    }

    /** 폭·글꼴 배율 조합마다 [content]를 새로 그려(key) [check]를 부른다 */
    private fun forEachCombo(
        widths: List<Dp> = WIDTHS,
        content: @Composable () -> Unit,
        check: (width: Dp, fontScale: Float) -> Unit
    ) {
        var combo by mutableStateOf(widths.first() to FONT_SCALES.first())
        composeRule.setContent {
            key(combo) { Frame(combo.first, combo.second, content) }
        }
        for (width in widths) for (fontScale in FONT_SCALES) {
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

    private fun textBounds(text: String): DpRect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertInside(label: String, inner: DpRect, outer: DpRect) {
        assertTrue(
            "$label: $inner 이(가) $outer 밖으로 나감",
            inner.left >= outer.left - TOLERANCE && inner.right <= outer.right + TOLERANCE &&
                inner.top >= outer.top - TOLERANCE && inner.bottom <= outer.bottom + TOLERANCE
        )
    }

    /**
     * 글자가 잘리지 않았는지: 모든 글자가 줄에 배치되고(maxLines에 걸려 잘린 글자 없음), 폭이 허용 폭 안이고, 글자 전체 높이가 Text 높이 안에 들어감
     * (hasVisualOverflow는 버튼 안 Text에서 폭이 남는데도 true가 나와 쓰지 않음 — SM-N986N 기기 테스트에서 확인)
     */
    private fun assertNotClipped(label: String, text: String) {
        val r = layoutOf(text)
        assertFalse("$label: 줄 수 제한에 걸려 잘림", r.multiParagraph.didExceedMaxLines)
        assertEquals("$label: 배치되지 않은 글자 있음", text.length, r.getLineEnd(r.lineCount - 1, visibleEnd = true))
        assertTrue("$label: 폭 넘침 ${r.size.width}/${r.layoutInput.constraints.maxWidth}", r.size.width <= r.layoutInput.constraints.maxWidth)
        // 고정 높이 안에서 줄바꿈되면 Text 높이는 칸에 맞춰 줄고 글자는 잘림 → 글자 전체 높이와 비교
        assertFalse("$label: 높이 넘침 ${r.multiParagraph.height}/${r.size.height}px", r.didOverflowHeight)
    }

    private fun height(rect: DpRect): Float = (rect.bottom - rect.top).value

    @Composable
    private fun ResultContent() {
        StudyResultContent(TIMESTAMP, "면접", "qa667", emptyList(), emptyList(), "0", ONE_HOUR_MS, reward = null, scrollable = false)
    }

    /** 결과 화면 날짜: 잘리지 않음. 16sp로 한 줄에 들어가면 한 줄, 안 들어가면 줄바꿈 */
    @Test
    fun resultTimestamp_neverClipped() {
        var measurer: TextMeasurer? = null
        var minStyle: TextStyle? = null
        forEachCombo(content = {
            ResultContent()
            // 기대값 계산용 — 지금 글꼴 배율로 16sp 한 줄 폭을 잰다
            measurer = rememberTextMeasurer()
            minStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp)
        }) { width, fontScale ->
            val r = layoutOf(TIMESTAMP)
            val minWidth = measurer!!.measure(TIMESTAMP, minStyle!!, maxLines = 1, softWrap = false).size.width
            val fitsAtMin = minWidth <= r.layoutInput.constraints.maxWidth
            val label = "폭 $width, 글꼴 $fontScale"
            assertNotClipped(label, TIMESTAMP)
            assertEquals("$label: 줄 수", if (fitsAtMin) 1 else 2, r.lineCount)
        }
    }

    /** 결과 화면 날짜: 글꼴 1.0·411dp에서는 수정 전처럼 22sp 한 줄 (줄었다면 22sp 한 줄 폭보다 좁아짐) */
    @Test
    fun resultTimestamp_sameAsBeforeAtDefaultFont() {
        var naturalWidth = 0
        composeRule.setContent {
            Frame(411.dp, 1.0f) {
                ResultContent()
                val style = MaterialTheme.typography.titleSmall.copy(fontSize = 22.sp)
                naturalWidth = rememberTextMeasurer().measure(TIMESTAMP, style, maxLines = 1, softWrap = false).size.width
            }
        }
        composeRule.waitForIdle()
        val r = layoutOf(TIMESTAMP)
        assertEquals(1, r.lineCount)
        assertEquals(naturalWidth, r.size.width)
    }

    /** 결과 화면 큰 시간(1시간 이상 HH:mm:ss): 한 줄, 잘리지 않음 */
    @Test
    fun resultTime_neverClipped() {
        forEachCombo(content = { ResultContent() }) { width, fontScale ->
            val r = layoutOf("01:00:00")
            val label = "폭 $width, 글꼴 $fontScale"
            assertNotClipped(label, "01:00:00")
            assertEquals("$label: 줄 수", 1, r.lineCount)
        }
    }

    /**
     * 학습 목표 확인 다이얼로그 버튼(DialogSmallButton): 글꼴 1.0에서 33dp(수정 전과 같음), 큰 글꼴에서 글자가 버튼 안에 들어감
     * Dialog 안에는 여기서 바꾼 fontScale이 적용되지 않아(기기 글꼴 배율을 따름 — 기기 테스트에서 확인) 버튼만 직접 그린다
     * (다이얼로그 전체는 실기기에서 글꼴 1.0·1.3·2.0으로 확인)
     */
    @Test
    fun goalCheckDialogButton() {
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) {
                DialogSmallButton(text = "종료", containerColor = MaterialTheme.colorScheme.primary, onClick = {})
            }
        }) { width, fontScale ->
            val button = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            if (fontScale == 1.0f) assertEquals("$label: 버튼 높이", 33f, height(button), TOLERANCE.value)
            assertInside(label, textBounds("종료"), button)
            assertNotClipped(label, "종료")
        }
    }

    /** 마이페이지 메뉴 버튼: 글꼴 1.0에서 50dp(수정 전과 같음), 큰 글꼴에서 글자가 버튼 안에 들어감 */
    @Test
    fun mypageMenuButton() {
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) { ButtonTemplate(text = MENU_TEXT, onClick = {}) }
        }) { width, fontScale ->
            val button = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            if (fontScale == 1.0f) assertEquals("$label: 버튼 높이", 50f, height(button), TOLERANCE.value)
            assertInside(label, textBounds(MENU_TEXT), button)
            assertNotClipped(label, MENU_TEXT)
        }
    }

    /** 학습 화면 버튼: 글꼴 1.0에서 150×70dp(수정 전과 같음), 큰 글꼴에서 줄바꿈된 글자가 버튼 안에 들어감 */
    @Test
    fun studyingStateChangeButton() {
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) {
                StateChangeButton(DEBUG_TEXT, MaterialTheme.colorScheme.errorContainer) {}
            }
        }) { width, fontScale ->
            val outer = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            if (fontScale == 1.0f) {
                assertEquals("$label: 너비", 150f, (outer.right - outer.left).value, TOLERANCE.value)
                assertEquals("$label: 높이", 70f, height(outer), TOLERANCE.value)
            }
            // 카드는 바깥 여백 10dp 안쪽
            val card = DpRect(outer.left + 10.dp, outer.top + 10.dp, outer.right - 10.dp, outer.bottom - 10.dp)
            assertInside(label, textBounds(DEBUG_TEXT), card)
            assertNotClipped(label, DEBUG_TEXT)
        }
    }

    /** 공부 중인 사람 카드: 최소 200dp, 큰 글꼴에서도 3명이 모두 카드 안에 들어감 */
    @Test
    fun studyingUserCard_fitsThreeUsers() {
        val users = (1..3).map { StudyingUser("u$it", "사용자$it", "0", "면접", 60_000L * it, null) }
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) { StudyingUserCard(users, "면접") }
        }) { width, fontScale ->
            val card = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            assertTrue("$label: 카드 높이 200dp 미만", height(card) >= 200f - TOLERANCE.value)
            users.forEach { assertInside("$label ${it.nickname}", textBounds(it.nickname), card) }
        }
    }

    /** 타이머: 폭이 250dp보다 좁으면 원이 폭에 맞춰 줄고, 시간 글자는 원 안에서 한 줄 */
    @Test
    fun timer_shrinksWithWidthAndTextFits() {
        forEachCombo(widths = listOf(300.dp, 200.dp), content = {
            Box(modifier = Modifier.testTag(TAG)) { SetTimer(time = TEN_HOURS_MS) }
        }) { width, fontScale ->
            val timer = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            val expected = minOf(width, 250.dp).value
            assertEquals("$label: 타이머 너비", expected, (timer.right - timer.left).value, TOLERANCE.value)
            assertEquals("$label: 타이머 높이", expected, height(timer), TOLERANCE.value)
            assertNotClipped(label, "10:00:00")
            assertEquals("$label: 줄 수", 1, layoutOf("10:00:00").lineCount)
        }
    }

    private companion object {
        const val TAG = "target"
        const val TIMESTAMP = "2026년 11월 22일 일요일 09:58 ~ 09:59"
        const val MENU_TEXT = "오픈소스 라이선스"
        const val DEBUG_TEXT = "+1h [DEBUG]"
        const val ONE_HOUR_MS = 3_600_000L
        const val TEN_HOURS_MS = 36_000_000L
        val WIDTHS = listOf(360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        // px → dp 반올림 오차
        val TOLERANCE = 0.5.dp
    }
}
