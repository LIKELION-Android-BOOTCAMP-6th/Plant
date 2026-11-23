package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.R
import com.a32b.plant.presentation.community.ui.PostTopBar
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BackTopBar를 쓰는 화면 9곳의 상단바가 좁은 화면·큰 글꼴에서도 겹치지 않고, TabTopBar와 같은 높이인지 확인 (#645)
 *
 * - 화면 폭은 requiredWidth로, 글꼴 배율은 LocalDensity의 fontScale로 바꾼다 (BottomBarLargeFontTest와 같은 방식)
 * - 글쓰기는 실제 PostTopBar를 그린다. 내 활동·공부 기록 상세의 actions는 ViewModel이 필요한 화면 안에 있어서
 *   같은 구성(버튼 종류·글자 스타일·아이콘 크기)을 여기서 다시 만든다. 화면 코드를 바꾸면 이 구성도 같이 바꿔야 한다
 */
@RunWith(AndroidJUnit4::class)
class BackTopBarLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class Scenario(
        val name: String,
        val title: String,
        val backDescription: String = "뒤로가기",
        // actions 안 요소를 찾는 방법 (merged 트리 기준: 버튼 하나가 노드 하나)
        val actionFinders: List<Finder> = emptyList(),
        // 글꼴 1.0(모든 폭)에서 제목이 말줄임 없이 다 보여야 하는지 (긴 화분 이름은 말줄임이 정상)
        val fitsAtDefault: Boolean = true,
        // 어떤 폭·글꼴에서도 말줄임 없이 다 보여야 하는 글자 (예: 선택 개수)
        val alwaysVisibleTexts: List<String> = emptyList(),
        // 글꼴 1.0에서는 말줄임 없이 다 보여야 하는 글자 (큰 글꼴에서는 말줄임 허용)
        val visibleAtDefaultTexts: List<String> = emptyList(),
        val bar: @Composable (Modifier) -> Unit,
    )

    private sealed class Finder(val key: String) {
        class Text(text: String) : Finder(text)
        class Description(desc: String) : Finder(desc)
    }

    private fun node(f: Finder): SemanticsNodeInteraction = when (f) {
        is Finder.Text -> composeRule.onNodeWithText(f.key)
        is Finder.Description -> composeRule.onNodeWithContentDescription(f.key)
    }

    @Composable
    private fun Frame(width: Dp, fontScale: Float, darkTheme: Boolean, scenario: Scenario) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = darkTheme) {
                Column(modifier = Modifier.requiredWidth(width)) {
                    scenario.bar(Modifier.testTag(BACK_BAR))
                    // 높이 비교용: 같은 제목의 하단바 탭 화면 앱바
                    TabTopBar(title = scenario.title, modifier = Modifier.testTag(TAB_BAR))
                }
            }
        }
    }

    // BackTopBar 쪽 제목 노드 (TabTopBar에도 같은 제목이 있어 BACK_BAR 안의 것을 고른다)
    private fun titleNode(title: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(title) and hasAnyAncestor(hasTestTag(BACK_BAR)), useUnmergedTree = true)

    private fun titleLayout(title: String): TextLayoutResult {
        val node = titleNode(title).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    private fun titleBounds(title: String): DpRect = titleNode(title).getUnclippedBoundsInRoot()

    private fun forEachCombo(darkTheme: Boolean = false, check: (Scenario, Dp, Float) -> Unit) {
        var combo by mutableStateOf(Triple(SCENARIOS.first(), WIDTHS.first(), FONT_SCALES.first()))
        composeRule.setContent {
            key(combo) { Frame(combo.second, combo.third, darkTheme, combo.first) }
        }
        // 첫 실패에서 멈추지 않고 모든 조합을 확인한 뒤 실패를 한꺼번에 보여준다
        val failures = mutableListOf<String>()
        for (s in SCENARIOS) for (w in WIDTHS) for (f in FONT_SCALES) {
            composeRule.runOnIdle { combo = Triple(s, w, f) }
            composeRule.waitForIdle()
            try {
                check(s, w, f)
            } catch (e: AssertionError) {
                failures += e.message.orEmpty()
            }
        }
        assertTrue("${failures.size}건 실패:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    private fun assertNoOverlap(s: Scenario, width: Dp, fontScale: Float) {
        val label = "${s.name}, 폭 $width, 글꼴 $fontScale"
        val bar = composeRule.onNodeWithTag(BACK_BAR).getUnclippedBoundsInRoot()
        val back = composeRule.onNodeWithContentDescription(s.backDescription).getUnclippedBoundsInRoot()
        val title = titleBounds(s.title)
        val actions = s.actionFinders.map { node(it).getUnclippedBoundsInRoot() }

        // 제목: 한 줄, 뒤로가기와 actions 사이
        assertEquals("$label: 제목 줄 수", 1, titleLayout(s.title).lineCount)
        assertTrue("$label: 제목이 뒤로가기와 겹침 (${title.left} < ${back.right})", title.left >= back.right - TOLERANCE.dp)
        actions.firstOrNull()?.let {
            assertTrue("$label: 제목이 actions와 겹침 (${title.right} > ${it.left})", title.right <= it.left + TOLERANCE.dp)
            assertTrue("$label: actions가 뒤로가기와 겹침", it.left >= back.right - TOLERANCE.dp)
        }
        // actions 요소끼리 겹치지 않고 화면 안에 있음
        actions.zipWithNext().forEach { (a, b) -> assertTrue("$label: actions 요소끼리 겹침", a.right <= b.left + TOLERANCE.dp) }
        actions.lastOrNull()?.let { assertTrue("$label: actions가 화면 밖으로 나감", it.right <= bar.right + TOLERANCE.dp) }
        // actions의 글자 버튼은 한 줄
        s.actionFinders.filterIsInstance<Finder.Text>().forEach {
            val r = mutableListOf<TextLayoutResult>().also { list ->
                composeRule.onNodeWithText(it.key, useUnmergedTree = true).fetchSemanticsNode()
                    .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(list)
            }.first()
            assertEquals("$label: '${it.key}' 버튼 글자 줄 수", 1, r.lineCount)
        }
        s.alwaysVisibleTexts.forEach { assertFullyVisible(label, it) }
    }

    private fun textLayout(text: String): TextLayoutResult = mutableListOf<TextLayoutResult>().also { list ->
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(list)
    }.first()

    private fun assertFullyVisible(label: String, text: String) {
        val r = textLayout(text)
        assertEquals("$label: '$text' 줄 수", 1, r.lineCount)
        assertFalse("$label: '$text' 말줄임됨", r.isLineEllipsized(0))
        assertEquals("$label: '$text' 배치되지 않은 글자", text.length, r.getLineEnd(0, visibleEnd = true))
    }

    @Test
    fun titleAndIconsDoNotOverlap() {
        forEachCombo { s, w, f -> assertNoOverlap(s, w, f) }
    }

    @Test
    fun titleAndIconsDoNotOverlap_darkTheme() {
        forEachCombo(darkTheme = true) { s, w, f -> assertNoOverlap(s, w, f) }
    }

    /** 높이는 같은 제목의 TabTopBar와 같다 (기본 64dp, 큰 글꼴로 제목이 더 크면 제목 높이) */
    @Test
    fun heightSameAsTabTopBar() {
        forEachCombo { s, w, f ->
            val back = composeRule.onNodeWithTag(BACK_BAR).fetchSemanticsNode().size.height
            val tab = composeRule.onNodeWithTag(TAB_BAR).fetchSemanticsNode().size.height
            assertEquals("${s.name}, 폭 $w, 글꼴 $f: 높이(px)", tab, back)
            if (f == 1.0f) {
                val px64 = with(composeRule.density) { 64.dp.roundToPx() }
                assertEquals("${s.name}, 폭 $w: 글꼴 1.0 높이 64dp", px64, back)
            }
        }
    }

    /**
     * 글꼴 1.0에서는 제목이 다 보이고(긴 화분 이름 제외), 가운데 정렬된다.
     * 단, 가운데에 두면 actions와 겹칠 만큼 actions가 넓으면 CenterAlignedTopAppBar가 제목을 왼쪽으로 옮긴다
     * (material3 1.4.0 AppBar.kt placeTopAppBar). 이때는 가운데보다 왼쪽에 있고 actions와 겹치지 않는지만 확인한다
     */
    @Test
    fun titleCenteredAtDefaultFont() {
        forEachCombo { s, w, f ->
            if (f != 1.0f || !s.fitsAtDefault) return@forEachCombo
            val label = "${s.name}, 폭 $w"
            val r = titleLayout(s.title)
            assertFalse("$label: 제목 말줄임됨", r.isLineEllipsized(0))
            assertEquals("$label: 배치되지 않은 글자", s.title.length, r.getLineEnd(0, visibleEnd = true))
            s.visibleAtDefaultTexts.forEach { assertFullyVisible(label, it) }
            val bar = composeRule.onNodeWithTag(BACK_BAR).getUnclippedBoundsInRoot()
            val title = titleBounds(s.title)
            val titleWidth = title.right - title.left
            val centeredRight = (bar.left + bar.right + titleWidth) / 2
            val actionsLeft = s.actionFinders.firstOrNull()?.let { node(it).getUnclippedBoundsInRoot().left }
            if (actionsLeft != null && centeredRight > actionsLeft + TOLERANCE.dp) {
                assertTrue("$label: actions가 넓으면 제목이 가운데보다 왼쪽으로 옮겨져야 함", title.right < centeredRight)
                assertTrue("$label: 옮겨진 제목이 actions와 겹침", title.right <= actionsLeft + TOLERANCE.dp)
            } else {
                val barCenter = (bar.left + bar.right) / 2
                val titleCenter = (title.left + title.right) / 2
                assertEquals("$label: 제목 가운데 정렬", barCenter.value, titleCenter.value, TOLERANCE)
            }
        }
    }

    /** backEnabled = false(화분 생성 저장 중 등)면 뒤로가기 버튼이 비활성이고 눌러도 onBack이 불리지 않는다 */
    @Test
    fun backDisabledDoesNotCallOnBack() {
        var enabled by mutableStateOf(false)
        var backCount = 0
        composeRule.setContent {
            PlantTheme { BackTopBar(title = "새 화분 만들기", onBack = { backCount++ }, backEnabled = enabled) }
        }
        val back = composeRule.onNodeWithContentDescription("뒤로가기")
        back.assertIsNotEnabled()
        back.performClick()
        composeRule.runOnIdle { assertEquals("비활성일 때 onBack 호출 횟수", 0, backCount) }

        // 저장이 끝나 다시 활성화되면 눌림
        composeRule.runOnIdle { enabled = true }
        back.assertIsEnabled()
        back.performClick()
        composeRule.runOnIdle { assertEquals("활성일 때 onBack 호출 횟수", 1, backCount) }
    }

    /**
     * 폭이 정해지지 않은 측정(IntrinsicSize)에서도 예외 없이 그려진다.
     * actions 폭 상한을 무한대 폭에서 빼면 Constraints로 나타낼 수 없는 값이 되어 앱이 종료됐다 (PR #744 리뷰)
     */
    @Test
    fun intrinsicWidthMeasureDoesNotCrash() {
        composeRule.setContent {
            PlantTheme {
                BackTopBar(
                    title = "내 활동",
                    onBack = {},
                    modifier = Modifier.width(IntrinsicSize.Max).testTag(BACK_BAR)
                ) {
                    TextButton(onClick = {}) { Text("선택", style = MaterialTheme.typography.titleMedium) }
                }
            }
        }
        composeRule.onNodeWithTag(BACK_BAR).assertExists()
        composeRule.onNode(hasText("내 활동") and hasAnyAncestor(hasTestTag(BACK_BAR)), useUnmergedTree = true).assertExists()
    }

    private companion object {
        const val BACK_BAR = "backTopBar"
        const val TAB_BAR = "tabTopBar"

        // 320dp(좁은 기기), 360dp(#673 이슈 기준), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
        val WIDTHS = listOf(320.dp, 360.dp, 411.dp)
        val FONT_SCALES = listOf(1.0f, 1.3f, 1.5f, 2.0f)
        // 위치 값을 px로 반올림할 때 생기는 오차 (0.5px 이하라 0.5dp 이하)
        const val TOLERANCE = 0.5f

        // "[태그] 화분 이름" — 화분 이름은 PotNameRule.MAX_LENGTH(15자)
        const val LONG_POT_TITLE = "[자격증] 정보처리기사실기대비하는새화분"

        @Composable
        private fun RowScope.StudyDetailActions(isShareMode: Boolean) {
            // StudyPlanDetailScreen의 actions와 같은 구성: 수정·공유·삭제, 공유 모드는 ✓만
            if (!isShareMode) {
                IconButton(onClick = {}) {
                    Icon(painterResource(R.drawable.ic_edit), "수정하기", modifier = Modifier.size(19.dp))
                }
            }
            IconButton(onClick = {}) {
                if (isShareMode) Icon(painterResource(R.drawable.ic_check), "확인", modifier = Modifier.size(19.dp))
                else Icon(painterResource(R.drawable.ic_share), "공유", modifier = Modifier.size(19.dp))
            }
            if (!isShareMode) {
                IconButton(onClick = {}) {
                    Icon(painterResource(R.drawable.ic_trash), "화분 전체 삭제하기", modifier = Modifier.size(24.dp))
                }
            }
        }

        @Composable
        private fun RowScope.ActivityActions(isSelectionMode: Boolean, selectedCount: Int = 99) {
            // CommunityActivityScreen의 actions와 같은 구성 (선택 모드: 가장 긴 문구 "북마크 취소" + "(n)" + 취소)
            if (isSelectionMode) {
                TextButton(onClick = {}, modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "북마크 취소", modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text("($selectedCount)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
                TextButton(onClick = {}) {
                    Text("취소", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            } else {
                TextButton(onClick = {}) {
                    Text("선택", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        private fun simple(title: String) = Scenario(title, title) { m -> BackTopBar(title = title, onBack = {}, modifier = m) }

        val SCENARIOS = listOf(
            simple("계정 관리"),
            simple("버그 제보"),
            simple("개선사항 제안"),
            simple("차단 목록"),
            simple("오픈소스 라이선스"),
            simple("새 화분 만들기"),
            Scenario("글쓰기", "글쓰기", actionFinders = listOf(Finder.Text("등록"))) { m ->
                Column(m) { PostTopBar(isEditMode = false, onBackClick = {}, onRegisterClick = {}) }
            },
            Scenario("글 수정 처리 중", "글 수정", actionFinders = listOf(Finder.Text("처리 중"))) { m ->
                Column(m) { PostTopBar(isEditMode = true, isSubmitting = true, onBackClick = {}, onRegisterClick = {}) }
            },
            Scenario("내 활동", "내 활동", actionFinders = listOf(Finder.Text("선택"))) { m ->
                BackTopBar(title = "내 활동", onBack = {}, modifier = m) { ActivityActions(isSelectionMode = false) }
            },
            Scenario(
                "내 활동 선택 모드", "내 활동",
                actionFinders = listOf(Finder.Text("북마크 취소"), Finder.Text("취소")),
                alwaysVisibleTexts = listOf("(99)", "취소"),
                visibleAtDefaultTexts = listOf("북마크 취소")
            ) { m ->
                BackTopBar(title = "내 활동", onBack = {}, modifier = m) { ActivityActions(isSelectionMode = true) }
            },
            Scenario(
                "내 활동 선택 모드 세 자리", "내 활동",
                actionFinders = listOf(Finder.Text("북마크 취소"), Finder.Text("취소")),
                alwaysVisibleTexts = listOf("(100)", "취소"),
                visibleAtDefaultTexts = listOf("북마크 취소")
            ) { m ->
                BackTopBar(title = "내 활동", onBack = {}, modifier = m) { ActivityActions(isSelectionMode = true, selectedCount = 100) }
            },
            Scenario(
                "공부 기록 상세", LONG_POT_TITLE,
                actionFinders = listOf(Finder.Description("수정하기"), Finder.Description("공유"), Finder.Description("화분 전체 삭제하기")),
                fitsAtDefault = false
            ) { m ->
                BackTopBar(title = LONG_POT_TITLE, onBack = {}, modifier = m) { StudyDetailActions(isShareMode = false) }
            },
            Scenario(
                "공부 기록 상세 공유 모드", LONG_POT_TITLE, backDescription = "공유 취소",
                actionFinders = listOf(Finder.Description("확인")),
                fitsAtDefault = false
            ) { m ->
                BackTopBar(
                    title = LONG_POT_TITLE, onBack = {}, modifier = m,
                    backIcon = painterResource(R.drawable.ic_study_result_close),
                    backContentDescription = "공유 취소"
                ) { StudyDetailActions(isShareMode = true) }
            },
        )
    }
}
