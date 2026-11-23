package com.a32b.plant.presentation.community.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 큰 글꼴에서 커뮤니티 목록 검색창의 안내 문구·입력 글자 아래쪽이 잘리지 않는지 확인 (#742),
 * 이모지 등 앱 글꼴에 없는 문자·좁은 화면에서도 잘리지 않는지 확인 (#749)
 *
 * - 화면 폭은 Box 폭(requiredWidth)으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다 (BottomBarLargeFontTest와 같은 방식)
 * - 입력 글자 잘림은 TextLayoutResult.didOverflowHeight로 본다 (글자 칸 높이가 글자 높이보다 작으면 true).
 *   안내 문구는 말줄임이 걸려도 true가 되므로 글자 칸 높이와 한 줄 높이를 직접 비교한다
 * - 입력 글자는 검색창 노드(SetText)의 TextLayoutResult, 안내 문구는 안내 문구 Text의 TextLayoutResult로 본다
 */
@RunWith(AndroidJUnit4::class)
class SearchBarSectionLargeFontTest {

    @get:Rule
    val composeRule = createComposeRule()

    // setContent는 한 테스트에서 한 번만 부를 수 있어, 바꿀 값을 상태로 두고 다시 그린다
    private var width by mutableStateOf(360.dp)
    private var fontScale by mutableStateOf(1.0f)
    private var darkTheme by mutableStateOf(false)
    private var content by mutableStateOf<@Composable () -> Unit>({})
    private var isContentSet = false

    private fun show(width: Dp, fontScale: Float, darkTheme: Boolean = false, content: @Composable () -> Unit) {
        if (!isContentSet) {
            isContentSet = true
            this.width = width
            this.fontScale = fontScale
            this.darkTheme = darkTheme
            this.content = content
            composeRule.setContent {
                key(this.width, this.fontScale, this.darkTheme, this.content) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, this.fontScale)) {
                        PlantTheme(darkTheme = this.darkTheme) {
                            Box(modifier = Modifier.requiredWidth(this.width)) {
                                // CommunityListScreen과 같은 좌우 10dp 여백
                                Column(modifier = Modifier.padding(horizontal = 10.dp)) { this@SearchBarSectionLargeFontTest.content() }
                            }
                        }
                    }
                }
            }
        } else {
            composeRule.runOnIdle {
                this.width = width
                this.fontScale = fontScale
                this.darkTheme = darkTheme
                this.content = content
            }
        }
        composeRule.waitForIdle()
    }

    private fun SemanticsNode.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        return results.first()
    }

    private fun fieldNode() = composeRule.onNode(hasSetTextAction()).fetchSemanticsNode()

    private fun placeholderNode() =
        composeRule.onNodeWithText(PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNode()

    /**
     * 안내 문구(빈 검색어)와 입력 글자 모두 높이가 넘치지 않고, 안내 문구가 한 줄로 검색창 안에 있다.
     * 앱 글꼴 글자만 쓴 검색어([QUERY])는 빈 검색어와 검색창 높이가 같다 (#749)
     */
    private fun assertNotClipped(label: String) {
        show(width, fontScale, darkTheme) { SearchBarSection("") {} }
        val placeholder = placeholderNode()
        val placeholderLayout = placeholder.textLayout()
        // 안내 문구는 말줄임(maxLines = 1)이 걸리면 didOverflowHeight가 true가 되므로 (didExceedMaxLines 포함),
        // 글자 칸 높이와 한 줄 높이만 비교한다
        assertFalse(
            "$label 안내 문구: 높이 넘침",
            placeholderLayout.size.height < placeholderLayout.multiParagraph.height
        )
        assertEquals("$label 안내 문구: 줄 수", 1, placeholderLayout.lineCount)
        val field = fieldNode().boundsInRoot
        val p = placeholder.boundsInRoot
        assertTrue("$label 안내 문구($p)가 검색창($field) 밖", p.top >= field.top && p.bottom <= field.bottom)

        for (query in listOf(QUERY) + FALLBACK_QUERIES) {
            show(width, fontScale, darkTheme) { SearchBarSection(query) {} }
            val node = fieldNode()
            assertFalse("$label, 검색어 '$query': 입력 글자 높이 넘침", node.textLayout().didOverflowHeight)
            if (query == QUERY) {
                assertEquals("$label: 앱 글꼴 검색어인데 빈 검색어와 높이가 다름", field.height, node.boundsInRoot.height)
            }
        }
    }

    @Test
    fun textNotClipped() {
        for (w in WIDTHS) for (f in FONT_SCALES) {
            width = w; fontScale = f; darkTheme = false
            assertNotClipped("폭 $w, 글꼴 $f")
        }
    }

    @Test
    fun textNotClipped_darkTheme() {
        for (w in WIDTHS) for (f in FONT_SCALES) {
            width = w; fontScale = f; darkTheme = true
            assertNotClipped("다크, 폭 $w, 글꼴 $f")
        }
    }

    /**
     * 글꼴 1.0에서는 수정 전(height 48dp 고정)과 아래 값이 px 단위로 같다
     * - 검색창 위치·크기, 안내 문구 위치·크기(빈 검색어), 안내 문구·입력 글자의 레이아웃 크기
     * - 입력 글자의 위치는 비교하지 않는다 (입력 글자로 노드를 찾아도 검색창 전체 범위가 나옴, SM-N986N에서 확인)
     * (수정 전 코드를 [LegacySearchBarSection]으로 옮겨 두고 같은 조건에서 비교)
     */
    @Test
    fun defaultFontMatchesLegacyLayout() {
        for (w in WIDTHS) for (query in listOf("", QUERY)) {
            val legacy = captureBounds(w, query) { LegacySearchBarSection(query) }
            val current = captureBounds(w, query) { SearchBarSection(query) {} }
            assertEquals("폭 $w, 검색어 '$query': 수정 전과 위치가 다름", legacy, current)
        }
    }

    /**
     * 빈 검색어 → 이모지 입력 → 지우기에서 검색창 높이가 늘었다가 처음 값으로 돌아온다 (#749)
     * - 아래 요소와의 겹침·위치 검사는 Column 배치 안에서 하므로, 실제 화면의 Scaffold topBar 배치는 검증하지 않는다
     *   (실제 화면에서 목록이 가려지지 않는 근거는 Scaffold가 topBar 높이로 본문 여백을 정하는 동작)
     */
    @Test
    fun heightFollowsQueryWithoutOverlap() {
        var query by mutableStateOf("")
        show(360.dp, 1.0f) {
            SearchBarSection(query) {}
            Box(modifier = Modifier.fillMaxWidth().height(10.dp).testTag(BELOW_TAG))
        }
        fun heights(): Pair<Float, Float> {
            val field = fieldNode().boundsInRoot
            val below = composeRule.onNodeWithTag(BELOW_TAG).fetchSemanticsNode().boundsInRoot
            assertTrue("아래 요소($below)가 검색창($field)과 겹침", below.top >= field.bottom)
            return field.height to below.top
        }
        val (emptyHeight, emptyBelowTop) = heights()
        composeRule.runOnIdle { query = "검색😀" }
        composeRule.waitForIdle()
        val (emojiHeight, emojiBelowTop) = heights()
        assertTrue("이모지 입력 후 검색창이 높아지지 않음 ($emptyHeight → $emojiHeight)", emojiHeight > emptyHeight)
        assertEquals("아래 요소가 검색창 높이 변화만큼 움직이지 않음", emojiHeight - emptyHeight, emojiBelowTop - emptyBelowTop)
        composeRule.runOnIdle { query = "" }
        composeRule.waitForIdle()
        assertEquals("지운 뒤 처음 높이·위치로 돌아오지 않음", emptyHeight to emptyBelowTop, heights())
    }

    private fun captureBounds(w: Dp, query: String, content: @Composable () -> Unit): List<Rect> {
        show(w, 1.0f, content = content)
        val field = fieldNode()
        val text = if (query.isEmpty()) placeholderNode().boundsInRoot else field.boundsInRoot
        val layout = if (query.isEmpty()) placeholderNode().textLayout() else field.textLayout()
        return listOf(
            field.boundsInRoot,
            text,
            Rect(0f, 0f, layout.size.width.toFloat(), layout.size.height.toFloat())
        )
    }

    private companion object {
        // 360dp(좁은 화면, #742의 945×1680 @420dpi), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭),
        // 280·300·310dp(#749: 글꼴 2.0에서 안내 문구가 280·300dp는 두 줄, 310dp는 한 줄이던 폭)
        val WIDTHS = listOf(280.dp, 300.dp, 310.dp, 360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        const val PLACEHOLDER = "검색어를 입력하세요"
        const val QUERY = "검색어 가나다"
        // 앱 글꼴에 없을 수 있는 문자가 든 검색어 (#749): 이모지 섞임, 이모지만, 한자, 특수문자
        val FALLBACK_QUERIES = listOf("검색😀", "😀", "漢字 검색", "★♪→①")
        const val BELOW_TAG = "below"
    }
}

/**
 * 수정 전(develop 2bb74cec) SearchBarSection을 옮긴 것 — 글꼴 1.0 회귀 비교용.
 * 입력 변경·아이콘 클릭 동작은 뺐고, 검색어 유무에 따른 아이콘 두 갈래는 Icon 하나에 조건으로 합쳤다 (모양·크기 값은 같음)
 */
@Composable
private fun LegacySearchBarSection(query: String) {
    OutlinedTextField(
        value = query,
        onValueChange = {},
        placeholder = {
            Text(
                "검색어를 입력하세요",
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        trailingIcon = {
            Icon(
                painter = painterResource(id = if (query.isNotEmpty()) R.drawable.ic_community_clear else R.drawable.ic_search),
                contentDescription = if (query.isNotEmpty()) "초기화" else "검색",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(if (query.isNotEmpty()) 18.dp else 20.dp),
                tint = MaterialTheme.colorScheme.onBackground
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        singleLine = true
    )
}
