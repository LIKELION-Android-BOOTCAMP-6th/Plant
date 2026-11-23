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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
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
 * 큰 글꼴에서 커뮤니티 목록 검색창의 안내 문구·입력 글자 아래쪽이 잘리지 않는지 확인 (#742)
 *
 * - 화면 폭은 Box 폭(requiredWidth)으로, 글꼴 배율은 LocalDensity의 fontScale로 바꿔 확인한다 (BottomBarLargeFontTest와 같은 방식)
 * - 잘림은 TextLayoutResult.didOverflowHeight로 본다 (글자 칸 높이가 글자 높이보다 작으면 true)
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

    /** 안내 문구(빈 검색어)와 입력 글자 모두 높이가 넘치지 않고, 안내 문구가 검색창 안에 있다 */
    private fun assertNotClipped(label: String) {
        show(width, fontScale, darkTheme) { SearchBarSection("") {} }
        val placeholder = placeholderNode()
        assertFalse("$label 안내 문구: 높이 넘침", placeholder.textLayout().didOverflowHeight)
        val field = fieldNode().boundsInRoot
        val p = placeholder.boundsInRoot
        assertTrue("$label 안내 문구($p)가 검색창($field) 밖", p.top >= field.top && p.bottom <= field.bottom)

        show(width, fontScale, darkTheme) { SearchBarSection(QUERY) {} }
        assertFalse("$label 입력 글자: 높이 넘침", fieldNode().textLayout().didOverflowHeight)
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
        // 360dp(좁은 화면, 이슈의 945×1680 @420dpi), 411dp(1080px ÷ 2.625 = 테스트 기기 SM-N986N 폭)
        val WIDTHS = listOf(360.dp, 411.dp)
        // 1.0 = 기본, 2.0 = 이슈의 최대. 사이 값은 비선형 배율 표(FontScaleConverterFactory)에 있는 배율
        val FONT_SCALES = listOf(1.0f, 1.15f, 1.3f, 1.5f, 1.8f, 2.0f)
        const val PLACEHOLDER = "검색어를 입력하세요"
        const val QUERY = "검색어 가나다"
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
