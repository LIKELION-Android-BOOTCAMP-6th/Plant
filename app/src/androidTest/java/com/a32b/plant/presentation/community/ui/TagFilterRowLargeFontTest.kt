package com.a32b.plant.presentation.community.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.TagChip
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 큰 글꼴에서 커뮤니티 목록 "태그 · 전체 선택 해제 · 공유글 보기 · 스위치" 줄이 겹치거나 잘리지 않는지 확인 (#742)
 *
 * - 화면 폭은 Box 폭(requiredWidth)으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다 (BottomBarLargeFontTest와 같은 방식)
 * - 화면처럼 좌우 10dp 여백(CommunityListScreen의 Column padding) 안에 그린다
 * - 스위치 모양의 자리는 [SHARED_SWITCH_TAG] 노드 범위로 본다 (scale(0.7f)이 반영된 범위)
 * - 누를 수 있는 범위·TalkBack이 읽는 노드는 "공유글 보기"와 스위치를 묶은 toggleable 노드(isToggleable)로 본다 (#750)
 */
@RunWith(AndroidJUnit4::class)
class TagFilterRowLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun forEachCombo(
        darkTheme: Boolean = false,
        isSharedShown: Boolean = false,
        check: (width: Dp, fontScale: Float) -> Unit
    ) {
        var combo by mutableStateOf(WIDTHS.first() to FONT_SCALES.first())
        composeRule.setContent {
            key(combo) {
                Frame(combo.first, combo.second, darkTheme) {
                    TagFilterRow(
                        isTagSheetShown = false,
                        isSharedShown = isSharedShown,
                        onTagClick = {},
                        onClearClick = {},
                        onSharedShownChange = {}
                    )
                }
            }
        }
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            composeRule.runOnIdle { combo = width to fontScale }
            composeRule.waitForIdle()
            check(width, fontScale)
        }
    }

    @Composable
    private fun Frame(width: Dp, fontScale: Float, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.requiredWidth(width)) {
                    Column(modifier = Modifier.padding(horizontal = SIDE_PADDING).testTag(ROW_AREA)) { content() }
                }
            }
        }
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val node = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    private fun textBounds(text: String): Rect =
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun switchBounds(): Rect =
        composeRule.onNodeWithTag(SHARED_SWITCH_TAG, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    // 수정 전 Switch는 자기 노드가 toggleable이라 이 노드 범위가 스위치 모양의 자리다
    private fun legacySwitchBounds(): Rect = composeRule.onNode(isToggleable()).fetchSemanticsNode().boundsInRoot

    private fun toggleBounds(): Rect = composeRule.onNode(isToggleable()).fetchSemanticsNode().boundsInRoot

    // 병합 트리에서 글자로 찾으면 그 글자를 품은 clickable 노드(태그 Row, 전체 선택 해제 Box)가 나온다
    private fun clickableBounds(text: String): Rect = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    private fun areaBounds(): Rect = composeRule.onNodeWithTag(ROW_AREA).fetchSemanticsNode().boundsInRoot

    /**
     * - 글자 3개(태그·전체 선택 해제·공유글 보기)가 한 줄로 다 보인다
     * - 스위치가 자리를 다 받았다: 자리가 모자라면 스위치 노드(isToggleable)는 좁아지지만 트랙은 52dp로 넘쳐 그려져
     *   아래 겹침 검사를 피해 가므로, 노드 폭이 [SWITCH_SCALE]배 한 트랙 폭과 같은지 먼저 본다
     * - 공유글 보기–스위치, (같은 줄이면) 전체 선택 해제–공유글 보기가 가로로 겹치지 않는다
     * - 글자 3개와 스위치가 좌우 여백 안쪽(화면 안)에 있다
     * - 스위치를 누르는 범위(toggleable 노드)가 여백 안쪽에 있고, 태그·전체 선택 해제를 누르는 범위와 겹치지 않는다 (#750)
     */
    private fun assertNoOverlapOrClip(label: String) {
        listOf(TAG_TEXT, CLEAR_TEXT, SHARED_TEXT).forEach {
            val r = layoutOf(it)
            assertEquals("$label $it: 줄 수", 1, r.lineCount)
            assertEquals("$label $it: 배치되지 않은 글자 있음", it.length, r.getLineEnd(0, visibleEnd = true))
        }
        val area = areaBounds()
        val tag = textBounds(TAG_TEXT)
        val clear = textBounds(CLEAR_TEXT)
        val shared = textBounds(SHARED_TEXT)
        val switch = switchBounds()
        val expectedSwitchWidth = with(composeRule.density) { SWITCH_TRACK_WIDTH.toPx() } * SWITCH_SCALE
        assertEquals("$label: 스위치 폭이 줄어듦 ($switch)", expectedSwitchWidth, switch.width, SWITCH_WIDTH_TOLERANCE_PX)
        listOf(TAG_TEXT to tag, CLEAR_TEXT to clear, SHARED_TEXT to shared, "스위치" to switch).forEach { (name, b) ->
            assertTrue("$label $name: 왼쪽이 여백 밖 ($b, 영역 $area)", b.left >= area.left)
            assertTrue("$label $name: 오른쪽이 여백 밖 ($b, 영역 $area)", b.right <= area.right)
        }
        assertTrue("$label: 공유글 보기($shared)가 스위치($switch)에 가려짐", shared.right <= switch.left)
        // 같은 줄에 있으면 앞쪽 묶음과 뒤쪽 묶음이 가로로 겹치지 않아야 한다
        if (shared.overlapsVertically(clear)) {
            assertTrue("$label: 전체 선택 해제($clear)와 공유글 보기($shared)가 겹침", clear.right <= shared.left)
        }
        val toggle = toggleBounds()
        assertTrue("$label: 누르는 범위($toggle)가 여백 밖 (영역 $area)", toggle.left >= area.left && toggle.right <= area.right)
        listOf(TAG_TEXT, CLEAR_TEXT).forEach {
            val other = clickableBounds(it)
            assertFalse("$label: 누르는 범위($toggle)가 $it($other)와 겹침", toggle.overlaps(other))
        }
    }

    private fun Rect.overlapsVertically(other: Rect) = top < other.bottom && other.top < bottom

    @Test
    fun noOverlapOrClip() {
        forEachCombo { width, fontScale -> assertNoOverlapOrClip("폭 $width, 글꼴 $fontScale") }
    }

    @Test
    fun noOverlapOrClip_switchOn() {
        forEachCombo(isSharedShown = true) { width, fontScale -> assertNoOverlapOrClip("켜짐, 폭 $width, 글꼴 $fontScale") }
    }

    @Test
    fun noOverlapOrClip_darkTheme() {
        forEachCombo(darkTheme = true) { width, fontScale -> assertNoOverlapOrClip("다크, 폭 $width, 글꼴 $fontScale") }
    }

    /** 글꼴 1.0에서는 한 줄 (공유글 보기와 태그가 같은 줄) */
    @Test
    fun defaultFontStaysOneLine() {
        forEachCombo { width, fontScale ->
            if (fontScale != 1.0f) return@forEachCombo
            assertTrue("폭 $width: 두 줄로 나뉨", textBounds(SHARED_TEXT).overlapsVertically(textBounds(TAG_TEXT)))
        }
    }

    /** 스위치 켜짐/꺼짐에서 스위치 자리가 같다 */
    @Test
    fun switchPositionSameForOnAndOff() {
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            val bounds = listOf(false, true).map { checked ->
                composeRule.setContentOnce(width, fontScale) { TagFilterRow(false, checked, {}, {}, {}) }
                switchBounds()
            }
            assertEquals("폭 $width, 글꼴 $fontScale: 켜짐/꺼짐 스위치 자리", bounds[0], bounds[1])
        }
    }

    /** 스위치·전체 선택 해제·태그를 누르면 각 콜백이 불린다 */
    @Test
    fun clicksCallCallbacks() {
        var tagClicks = 0
        var clearClicks = 0
        var sharedShown by mutableStateOf(false)
        composeRule.setContent {
            Frame(360.dp, 2.0f) {
                TagFilterRow(
                    isTagSheetShown = false,
                    isSharedShown = sharedShown,
                    onTagClick = { tagClicks++ },
                    onClearClick = { clearClicks++ },
                    onSharedShownChange = { sharedShown = !sharedShown }
                )
            }
        }
        composeRule.onNode(isToggleable()).performClick()
        composeRule.runOnIdle { assertTrue("스위치를 눌러도 켜지지 않음", sharedShown) }
        composeRule.onNode(isToggleable()).performClick()
        composeRule.runOnIdle { assertFalse("스위치를 다시 눌러도 꺼지지 않음", sharedShown) }
        composeRule.onNodeWithText(CLEAR_TEXT, useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(TAG_TEXT, useUnmergedTree = true).performClick()
        composeRule.runOnIdle {
            assertEquals("전체 선택 해제 콜백 횟수", 1, clearClicks)
            assertEquals("태그 콜백 횟수", 1, tagClicks)
            assertFalse("전체 선택 해제·태그를 눌렀는데 스위치가 바뀜", sharedShown)
        }
        // 스위치 모양 가운데와 "공유글 보기" 글자를 눌러도 바뀐다 (#750)
        composeRule.onNodeWithTag(SHARED_SWITCH_TAG, useUnmergedTree = true).performClick()
        composeRule.runOnIdle { assertTrue("스위치 모양을 눌러도 켜지지 않음", sharedShown) }
        composeRule.onNodeWithText(SHARED_TEXT, useUnmergedTree = true).performClick()
        composeRule.runOnIdle { assertFalse("공유글 보기 글자를 눌러도 꺼지지 않음", sharedShown) }
    }

    /**
     * 스위치 모양 가운데에서 위·아래·왼쪽·오른쪽으로 [TOUCH_OFFSETS]만큼 떨어진 곳을 누르면 바뀐다 (#750)
     * - 48dp 범위면 가운데에서 24dp까지 눌려야 하므로, px 반올림 여유를 두고 23dp까지 누른다
     * - 누를 때마다 켜짐↔꺼짐이 바뀌므로 두 상태 모두에서 누르게 된다
     * - 누르는 범위(toggleable 노드)가 가로·세로 48dp 이상인지도 본다
     */
    @Test
    fun touchTargetAtLeast48dp() {
        var shown by mutableStateOf(false)
        var toggles = 0
        val onChange: () -> Unit = {
            shown = !shown
            toggles++
        }
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            composeRule.setContentOnce(width, fontScale) { TagFilterRow(false, shown, {}, {}, onChange) }
            val label = "폭 $width, 글꼴 $fontScale"
            val minPx = with(composeRule.density) { MIN_TOUCH_SIZE.toPx() }
            val toggle = toggleBounds()
            assertTrue("$label: 누르는 범위 폭이 48dp 미만 ($toggle)", toggle.width >= minPx - SWITCH_WIDTH_TOLERANCE_PX)
            assertTrue("$label: 누르는 범위 높이가 48dp 미만 ($toggle)", toggle.height >= minPx - SWITCH_WIDTH_TOLERANCE_PX)

            val center = switchBounds().center
            val points = listOf("가운데" to Offset.Zero) + TOUCH_OFFSETS.flatMap { d ->
                val px = with(composeRule.density) { d.toPx() }
                listOf(
                    "위 $d" to Offset(0f, -px), "아래 $d" to Offset(0f, px),
                    "왼쪽 $d" to Offset(-px, 0f), "오른쪽 $d" to Offset(px, 0f)
                )
            }
            points.forEach { (where, offset) ->
                val (togglesBefore, shownBefore) = composeRule.runOnIdle { toggles to shown }
                composeRule.onRoot().performTouchInput { click(center + offset) }
                composeRule.runOnIdle {
                    assertEquals("$label, $where (켜짐=$shownBefore 에서 누름): 안 바뀜", togglesBefore + 1, toggles)
                }
            }
        }
    }

    /** TalkBack이 읽는 노드가 하나이고, "공유글 보기"·스위치 역할·켜짐/꺼짐을 함께 갖는다 (#750) */
    @Test
    fun switchHasLabelForAccessibility() {
        var shown by mutableStateOf(false)
        composeRule.setContentOnce(360.dp, 1.0f) { TagFilterRow(false, shown, {}, {}, {}) }
        // 스위치가 따로 초점을 받는 노드를 만들지 않는다
        composeRule.onAllNodes(isToggleable(), useUnmergedTree = true).assertCountEquals(1)
        val node = composeRule.onNode(isToggleable())
        node.assert(hasText(SHARED_TEXT))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff()
        composeRule.runOnIdle { shown = true }
        node.assertIsOn()
    }

    /**
     * 글꼴 1.0에서는 수정 전 Row와 위치·크기가 px 단위로 같다
     * (수정 전 코드를 [LegacyTagFilterRow]로 옮겨 두고 같은 조건에서 비교)
     */
    @Test
    fun defaultFontMatchesLegacyLayout() {
        for (width in WIDTHS) for (checked in listOf(false, true)) {
            val legacy = captureBounds(width, ::legacySwitchBounds) { LegacyTagFilterRow(checked) }
            val current = captureBounds(width, ::switchBounds) { TagFilterRow(false, checked, {}, {}, {}) }
            assertEquals("폭 $width, 켜짐=$checked: 수정 전과 위치가 다름", legacy, current)
        }
    }

    private fun captureBounds(width: Dp, switchBoundsOf: () -> Rect, content: @Composable () -> Unit): List<Rect> {
        composeRule.setContentOnce(width, 1.0f, content)
        return listOf(
            areaBounds(),
            textBounds(TAG_TEXT),
            composeRule.onNode(hasContentDescription(TAG_ICON_DESC), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot,
            textBounds(CLEAR_TEXT),
            textBounds(SHARED_TEXT),
            switchBoundsOf()
        )
    }

    // setContent는 한 테스트에서 한 번만 부를 수 있어, 바꿀 값을 상태로 두고 다시 그린다
    private var slot by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var slotWidth by mutableStateOf(360.dp)
    private var slotFontScale by mutableStateOf(1.0f)
    private var isContentSet = false

    private fun ComposeContentTestRule.setContentOnce(
        width: Dp,
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        if (!isContentSet) {
            isContentSet = true
            slot = content
            slotWidth = width
            slotFontScale = fontScale
            setContent {
                val w = slotWidth
                val f = slotFontScale
                val c = slot
                key(w, f, c) { Frame(w, f) { c?.invoke() } }
            }
        } else {
            runOnIdle {
                slot = content
                slotWidth = width
                slotFontScale = fontScale
            }
        }
        waitForIdle()
    }

    private companion object {
        // 360dp(좁은 화면, 이슈의 945×1680 @420dpi), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
        val WIDTHS = listOf(360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        val SIDE_PADDING = 10.dp
        const val ROW_AREA = "row_area"
        const val TAG_TEXT = "태그"
        const val CLEAR_TEXT = "전체 선택 해제"
        const val SHARED_TEXT = "공유글 보기"
        const val TAG_ICON_DESC = "태그박스"
        // material3 1.4.0 SwitchTokens.TrackWidth, CommunityListScreen의 Modifier.scale(0.7f)
        val SWITCH_TRACK_WIDTH = 52.dp
        const val SWITCH_SCALE = 0.7f
        // 노드 범위가 px로 반올림되며 생기는 오차
        const val SWITCH_WIDTH_TOLERANCE_PX = 1f
        // Material 권장 최소 터치 크기 (material3 LocalMinimumInteractiveComponentSize 기본값)
        val MIN_TOUCH_SIZE = 48.dp
        // 이슈 #750에서 잰 거리(12·16·20dp)와, 48dp 범위의 끝(가운데에서 24dp)에서 px 반올림 여유 1dp를 뺀 23dp
        val TOUCH_OFFSETS = listOf(12.dp, 16.dp, 20.dp, 23.dp)
    }
}

/**
 * 수정 전(develop 2bb74cec) CommunityListScreen의 같은 줄을 옮긴 것 — 글꼴 1.0 회귀 비교용.
 * 클릭 동작은 빈 람다, 태그 아이콘은 태그 선택 창이 닫힌 상태(ic_down)로 고정했다.
 * "공유글 보기"·Switch 부분은 #750 수정 전 TagFilterRow와도 같아서, #750의 스위치 모양 회귀 비교에도 쓴다
 */
@Composable
private fun LegacyTagFilterRow(isSharedShown: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable { }
        ) {
            Text(
                "태그",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_down),
                contentDescription = "태그박스",
                modifier = Modifier.size(18.dp)
            )
        }
        Box(modifier = Modifier.clickable { }) {
            TagChip("전체 선택 해제", false)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "공유글 보기",
            style = MaterialTheme.typography.titleMedium
        )
        Switch(
            isSharedShown,
            onCheckedChange = { },
            modifier = Modifier
                .scale(0.7f)
                .padding(end = 10.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
