package com.a32b.plant.presentation.studying.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
 *   기기 폭보다 넓은 값도 그대로 쓰도록 requiredWidth로 정한다 (width는 기기 폭에 맞춰 줄어듦)
 * - 글꼴 1.0에서는 수정 전과 같은 크기인지도 확인한다 (자동 크기 조절 글자는 기기 dpi에 따라 한 단계(0.25sp) 작은 것까지 허용)
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
                Box(modifier = Modifier.requiredWidth(width)) { content() }
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
     * 글자가 잘리지 않았는지: 모든 글자가 줄에 배치되고(maxLines에 걸려 잘린 글자 없음), 글자 전체 높이가 Text 높이 안에 들어감
     * - softWrap이 기본값(true)이면 가로로 넘치는 글자는 줄바꿈되거나 maxLines에 걸리므로 위 두 검사로 잡힌다 (이 테스트에서 확인하는 Text는 모두 기본값)
     *   softWrap = false인 Text는 이 판정으로 가로 넘침을 못 잡음. TextLayoutResult.size는 제약 안으로 맞춰져 폭 비교로도 못 잡음
     * - hasVisualOverflow는 쓰지 않음: semantics(GetTextLayoutResult)로 받은 결과에서, 360dp·글꼴 1.0 마이페이지 메뉴 버튼 Text가 폭 218/819px인데도 true였음 (SM-N986N 기기 테스트 출력)
     */
    private fun assertNotClipped(label: String, text: String) {
        val r = layoutOf(text)
        assertFalse("$label: 줄 수 제한에 걸려 잘림", r.multiParagraph.didExceedMaxLines)
        assertEquals("$label: 배치되지 않은 글자 있음", text.length, r.getLineEnd(r.lineCount - 1, visibleEnd = true))
        // 칸 높이보다 글자가 높으면 Text 높이는 칸에 맞춰 줄고 글자는 잘림 → 글자 전체 높이와 비교
        assertFalse("$label: 높이 넘침 ${r.multiParagraph.height}/${r.size.height}px", r.didOverflowHeight)
    }

    private fun height(rect: DpRect): Float = (rect.bottom - rect.top).value

    /** 결과 화면처럼 바깥 여백 10dp를 두고 그린다 (StudyResultScreen의 Column padding) — 그래야 폭이 실기기와 같음 */
    @Composable
    private fun ResultContent() {
        Box(modifier = Modifier.padding(10.dp)) {
            StudyResultContent(TIMESTAMP, "면접", "qa667", emptyList(), emptyList(), "0", ONE_HOUR_MS, reward = null, scrollable = false)
        }
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

    /**
     * 결과 화면 날짜: 실기기(SM-N986N)에서 본 줄 수와 같은지 — 위 테스트의 기대값이 코드와 같은 방식으로 계산돼서 따로 고정값으로 확인
     * 실기기 확인: 411dp 글꼴 1.0·1.3 한 줄, 2.0 두 줄 / 360dp(wm size 945x1680) 글꼴 1.0 한 줄, 1.3·2.0 두 줄
     */
    @Test
    fun resultTimestamp_matchesDeviceLineCount() {
        val expected = mapOf(
            (360.dp to 1.0f) to 1, (360.dp to 1.3f) to 2, (360.dp to 2.0f) to 2,
            (411.dp to 1.0f) to 1, (411.dp to 1.3f) to 1, (411.dp to 2.0f) to 2
        )
        var checked = 0
        forEachCombo(content = { ResultContent() }) { width, fontScale ->
            val lines = expected[width to fontScale] ?: return@forEachCombo
            assertEquals("폭 $width, 글꼴 $fontScale: 줄 수", lines, layoutOf(TIMESTAMP).lineCount)
            checked++
        }
        // WIDTHS·FONT_SCALES가 바뀌어 고정값 조합이 빠지면 아무것도 확인하지 않고 통과하지 않도록
        assertEquals("확인한 조합 수", expected.size, checked)
    }

    /**
     * 결과 화면 날짜: 글꼴 1.0·411dp에서는 수정 전처럼 22sp 한 줄 (폭 때문에 줄어들지 않음)
     * 자동 크기 조절은 px 단위 float로 계산해서, 기기 dpi에 따라 글꼴 1.0에서도 한 단계(0.25sp) 작게 나올 수 있다
     * (TextAutoSize.kt 계산을 따라 해 보면 420·440·480dpi는 22sp, 448·453dpi는 21.75sp) → 21.75sp~22sp 한 줄 폭 사이면 통과
     */
    @Test
    fun resultTimestamp_sameAsBeforeAtDefaultFont() {
        var maxWidth = 0
        var oneStepSmallerWidth = 0
        composeRule.setContent {
            Frame(411.dp, 1.0f) {
                ResultContent()
                val measurer = rememberTextMeasurer()
                val style = MaterialTheme.typography.titleSmall
                maxWidth = measurer.measure(TIMESTAMP, style.copy(fontSize = 22.sp), maxLines = 1, softWrap = false).size.width
                oneStepSmallerWidth = measurer.measure(TIMESTAMP, style.copy(fontSize = 21.75.sp), maxLines = 1, softWrap = false).size.width
            }
        }
        composeRule.waitForIdle()
        val r = layoutOf(TIMESTAMP)
        assertEquals(1, r.lineCount)
        // float 계산·글자 배치 차이로 21.75sp 한 줄 폭과 1px 안팎 다를 수 있어 아래쪽은 1px 여유
        assertTrue(
            "폭 ${r.size.width}px가 21.75sp(${oneStepSmallerWidth}px)~22sp(${maxWidth}px) 한 줄 폭 밖",
            r.size.width in (oneStepSmallerWidth - 1)..maxWidth
        )
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
     * (다이얼로그 전체는 실기기에서 글꼴 1.0·1.3·2.0으로 확인 — 버튼 세로 여백을 6dp로 줄였던 중간 버전으로 확인함)
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

    /**
     * 학습 화면 버튼: 글꼴 1.0에서 150×70dp, 큰 글꼴에서 줄바꿈된 글자가 버튼 안에 들어감
     * 높이는 바깥 여백 10dp 두 번과 안쪽 최소 50dp를 Compose가 각각 px로 반올림해 더하므로, 같은 방식으로 계산한 px와 정확히 비교한다
     * (dp 허용 오차로 비교하면 틀리게 실패할 수 있음: 0.5dp면 반올림 3번이 쌓여 넘고(2.75 기기: 194px ≈ 70.55dp),
     *  1.5px로 넓혀도 정확히 1.5px 차이 나는 경우 px→dp 변환의 float 오차로 넘음.
     *  수정 전 height(70.dp)는 한 번만 반올림 — 2.625 기기에서 수정 전 184px, 수정 후 183px)
     */
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
                val sizePx = composeRule.onNodeWithTag(TAG).fetchSemanticsNode().size
                with(composeRule.density) {
                    assertEquals("$label: 너비(px)", 150.dp.roundToPx(), sizePx.width)
                    assertEquals("$label: 높이(px)", 2 * 10.dp.roundToPx() + 50.dp.roundToPx(), sizePx.height)
                }
            }
            // 카드는 바깥 여백 10dp 안쪽
            val card = DpRect(outer.left + 10.dp, outer.top + 10.dp, outer.right - 10.dp, outer.bottom - 10.dp)
            assertInside(label, textBounds(DEBUG_TEXT), card)
            assertNotClipped(label, DEBUG_TEXT)
        }
    }

    /**
     * 공부 중인 사람 카드: 최소 200dp, 3명의 닉네임·공부 시간 글자가 모두 잘리지 않음
     * (카드가 200dp 고정이고 내용이 그보다 길면 마지막 행부터 남은 높이로 눌려, 위치는 카드 안이어도 글자 아래가 잘린다 → 글자마다 잘림 검사.
     *  예전 코드를 복사한 임시 테스트(360dp)에서는 글꼴 1.8·2.0일 때 3번째 닉네임이 잘렸고, 1.0에서는 마지막 여백 8dp가 넘친 만큼을 흡수해 잘리지 않았음)
     */
    @Test
    fun studyingUserCard_fitsThreeUsers() {
        val users = (1..3).map { StudyingUser("u$it", "사용자$it", "0", "면접", 60_000L * it, null) }
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) { StudyingUserCard(users, "면접") }
        }) { width, fontScale ->
            val card = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            assertTrue("$label: 카드 높이 200dp 미만", height(card) >= 200f - TOLERANCE.value)
            users.forEachIndexed { index, user ->
                // StudyingUserItem의 공부 시간 글자 (formatToMinute: 60_000ms × n → "n분")
                val timeText = " ${index + 1}분 째 공부중!"
                for (text in listOf(user.nickname, timeText)) {
                    assertInside("$label $text", textBounds(text), card)
                    assertNotClipped("$label $text", text)
                }
            }
        }
    }

    /**
     * 공부 중인 사람 카드, 닉네임 10자(NicknameRule.MAX_LENGTH): 공부 시간 글자가 쪼개지지 않고 한 줄 (#736)
     * 닉네임과 한 줄에 안 들어가면 공부 시간은 통째로 다음 줄, 닉네임은 한 줄(넘치면 말줄임)
     */
    @Test
    fun studyingUserCard_longNickname() {
        val users = (1..3).map { StudyingUser("u$it", "가나다라마바사아자$it", "0", "면접", 132 * 60_000L, null) }
        forEachCombo(content = {
            Box(modifier = Modifier.testTag(TAG)) { StudyingUserCard(users, "면접") }
        }) { width, fontScale ->
            val card = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val label = "폭 $width, 글꼴 $fontScale"
            assertTrue("$label: 카드 높이 200dp 미만", height(card) >= 200f - TOLERANCE.value)
            // 세 줄이 같은 글자라 첫 번째 사람의 공부 시간만 확인 (formatToMinute: 132분)
            val time = composeRule.onAllNodesWithText(LONG_TIME_TEXT, useUnmergedTree = true)
            time.assertCountEquals(3)
            users.forEach { user ->
                val r = layoutOf(user.nickname)
                assertEquals("$label ${user.nickname}: 줄 수", 1, r.lineCount)
                assertFalse("$label ${user.nickname}: 높이 넘침", r.didOverflowHeight)
                assertInside("$label ${user.nickname}", textBounds(user.nickname), card)
            }
            val timeNode = time[0].fetchSemanticsNode()
            val results = mutableListOf<TextLayoutResult>()
            timeNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
            val timeLayout = results.first()
            assertEquals("$label 공부 시간: 줄 수", 1, timeLayout.lineCount)
            assertEquals("$label 공부 시간: 배치되지 않은 글자 있음", LONG_TIME_TEXT.length, timeLayout.getLineEnd(0, visibleEnd = true))
            assertFalse("$label 공부 시간: 높이 넘침", timeLayout.didOverflowHeight)
            assertInside("$label 공부 시간", time[0].getUnclippedBoundsInRoot(), card)
        }
    }

    /** 공부 중인 사람 카드, 짧은 닉네임·글꼴 1.0: 수정 전처럼 닉네임 바로 뒤 같은 줄에 공부 시간 (#736) */
    @Test
    fun studyingUserCard_shortNicknameSameLine() {
        val user = StudyingUser("u1", "사용자1", "0", "면접", 60_000L, null)
        forEachCombo(content = { StudyingUserCard(listOf(user), "면접") }) { width, fontScale ->
            if (fontScale != 1.0f) return@forEachCombo
            val nickname = textBounds(user.nickname)
            val time = textBounds(" 1분 째 공부중!")
            val label = "폭 $width"
            assertEquals("$label: 공부 시간이 닉네임 바로 뒤", nickname.right.value, time.left.value, TOLERANCE.value)
            assertEquals("$label: 공부 시간 줄 수", 1, layoutOf(" 1분 째 공부중!").lineCount)
        }
    }

    /**
     * 학습 화면 "일시정지" 표시:글꼴 1.0에서 20dp(수정 전과 같음), 큰 글꼴에서 잘리지 않음,
     * 숨겼을 때도 높이가 같고(아래 버튼이 움직이지 않음) 접근성 트리에 나오지 않음
     */
    @Test
    fun pausedLabel() {
        forEachCombo(widths = listOf(360.dp), content = {
            Column {
                Box(modifier = Modifier.testTag(TAG)) { PausedLabel(visible = true) }
                Box(modifier = Modifier.testTag(HIDDEN_TAG)) { PausedLabel(visible = false) }
            }
        }) { _, fontScale ->
            val label = "글꼴 $fontScale"
            val shown = composeRule.onNodeWithTag(TAG).getUnclippedBoundsInRoot()
            val hidden = composeRule.onNodeWithTag(HIDDEN_TAG).getUnclippedBoundsInRoot()
            if (fontScale == 1.0f) assertEquals("$label: 높이", 20f, height(shown), TOLERANCE.value)
            assertEquals("$label: 숨겼을 때 높이", height(shown), height(hidden), TOLERANCE.value)
            composeRule.onAllNodesWithText(PAUSED_TEXT, useUnmergedTree = true).assertCountEquals(1)
            assertNotClipped(label, PAUSED_TEXT)
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
        const val HIDDEN_TAG = "hidden"
        const val PAUSED_TEXT = "일시정지"
        const val TIMESTAMP = "2026년 11월 22일 일요일 09:58 ~ 09:59"
        const val MENU_TEXT = "오픈소스 라이선스"
        const val LONG_TIME_TEXT = " 132분 째 공부중!"
        const val DEBUG_TEXT = "+1h [DEBUG]"
        const val ONE_HOUR_MS = 3_600_000L
        const val TEN_HOURS_MS = 36_000_000L
        val WIDTHS = listOf(360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        // 크기·위치 값 하나를 px로 반올림할 때 생기는 오차 (0.5px 이하라 0.5dp 이하)
        val TOLERANCE = 0.5.dp
    }
}
