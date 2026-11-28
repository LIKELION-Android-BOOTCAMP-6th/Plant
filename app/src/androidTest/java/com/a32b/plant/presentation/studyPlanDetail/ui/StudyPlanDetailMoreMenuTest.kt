package com.a32b.plant.presentation.studyPlanDetail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.BackTopBar
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 화분 상세 상단바 더보기 메뉴 [StudyPlanDetailMoreMenu]와 작아진 제목 (#808)
 *
 * - 메뉴 항목은 위에서부터 수정·공유·삭제, 누르면 메뉴가 닫히고 해당 동작이 한 번만 불림
 * - 메뉴 항목 글자가 한 줄로 다 보이고, 항목이 글자보다 낮지 않음 (큰 글꼴은 기기 설정을 바꿔 실행)
 * - 긴 화분 이름: 바꾸기 전 구성(displayLarge + 아이콘 3개)보다 제목 글자가 더 많이 보임
 */
@RunWith(AndroidJUnit4::class)
class StudyPlanDetailMoreMenuTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val labels = listOf("수정", "공유", "삭제")

    private fun textLayout(text: String): TextLayoutResult = mutableListOf<TextLayoutResult>().also { list ->
        composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(list)
    }.first()

    @Test
    fun menuItemsInOrderAndEachCallsItsActionOnceThenCloses() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            PlantTheme {
                StudyPlanDetailMoreMenu(
                    onEdit = { calls += "수정" },
                    onShare = { calls += "공유" },
                    onDelete = { calls += "삭제" }
                )
            }
        }

        // 처음에는 메뉴가 닫혀 있음
        labels.forEach { composeRule.onNodeWithText(it).assertDoesNotExist() }

        for (label in labels) {
            composeRule.onNodeWithContentDescription("더보기").assertHasClickAction().performClick()
            composeRule.waitForIdle()

            // 항목 순서: 위에서부터 수정·공유·삭제 (#645 의도 유지)
            val tops = labels.map { composeRule.onNodeWithText(it).getUnclippedBoundsInRoot().top }
            assertEquals("메뉴 항목 순서", tops.sorted(), tops)

            composeRule.onNode(hasText(label) and hasClickAction()).performClick()
            composeRule.waitForIdle()

            // 메뉴가 닫히고 누른 항목의 동작만 한 번 불림
            labels.forEach { composeRule.onNodeWithText(it).assertDoesNotExist() }
            assertEquals("'$label' 누른 뒤 불린 동작", listOf(label), calls)
            calls.clear()
        }
    }

    /**
     * 메뉴 항목 글자가 한 줄로 다 보이고, 항목이 글자보다 낮지 않다.
     * 메뉴는 팝업이라 테스트에서 LocalDensity로 준 글꼴 배율이 닿지 않는다 (플립3에서 확인).
     * 그래서 기기 글꼴 배율 그대로 확인하고, 큰 글꼴은 기기 설정(font_scale 2.0)을 바꿔 이 테스트를 다시 돌린다
     */
    @Test
    fun menuItemsFitTextAtDeviceFontScale() {
        composeRule.setContent {
            PlantTheme {
                StudyPlanDetailMoreMenu(onEdit = {}, onShare = {}, onDelete = {})
            }
        }
        composeRule.onNodeWithContentDescription("더보기").performClick()
        composeRule.waitForIdle()

        val fontScale = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.fontScale
        labels.forEach { label ->
            val r = textLayout(label)
            assertEquals("글꼴 $fontScale '$label' 줄 수", 1, r.lineCount)
            assertFalse("글꼴 $fontScale '$label' 말줄임됨", r.isLineEllipsized(0))
            val text = composeRule.onNodeWithText(label, useUnmergedTree = true).getUnclippedBoundsInRoot()
            val item = composeRule.onNode(hasText(label) and hasClickAction()).getUnclippedBoundsInRoot()
            val textHeight = (text.bottom - text.top).value
            val itemHeight = (item.bottom - item.top).value
            assertTrue("글꼴 $fontScale '$label' 항목($itemHeight dp)이 글자($textHeight dp)보다 낮음", itemHeight >= textHeight)
            // 정렬 메뉴(PotSortDropdown)처럼 글자는 왼쪽 12dp에 붙고, 오른쪽에 36dp 이상 여백 (#808)
            // 글꼴 1.0에서는 기본 최소 폭 112dp보다 좁음
            val itemWidth = (item.right - item.left).value
            val leftGap = (text.left - item.left).value
            val rightGap = (item.right - text.right).value
            assertEquals("글꼴 $fontScale '$label' 글자 왼쪽 여백(dp)", 12f, leftGap, 0.5f)
            assertTrue("글꼴 $fontScale '$label' 글자 오른쪽 여백 ${rightGap}dp", rightGap >= 36f - 0.5f)
            if (fontScale <= 1.0f) assertTrue("글꼴 1.0 '$label' 항목 폭 $itemWidth dp가 기본 112dp보다 좁지 않음", itemWidth < 112f)
        }
    }

    /** 360dp(#673 기준 폭)·글꼴 1.0에서 긴 화분 이름이 바꾸기 전보다 더 많이 보인다 */
    @Test
    fun longPotTitleShowsMoreCharactersThanBefore() {
        var useNew by mutableStateOf(false)
        composeRule.setContent {
            PlantTheme {
                Column(modifier = Modifier.requiredWidth(360.dp)) {
                    if (useNew) {
                        BackTopBar(
                            title = LONG_POT_TITLE, onBack = {},
                            titleStyle = MaterialTheme.typography.titleMedium
                        ) { StudyPlanDetailMoreMenu(onEdit = {}, onShare = {}, onDelete = {}) }
                    } else {
                        // 바꾸기 전 구성: displayLarge 제목 + 수정(19dp)·공유(19dp)·삭제(24dp) 아이콘 버튼
                        BackTopBar(title = LONG_POT_TITLE, onBack = {}) {
                            IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_edit), "수정하기", modifier = Modifier.size(19.dp)) }
                            IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_share), "공유", modifier = Modifier.size(19.dp)) }
                            IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_trash), "화분 전체 삭제하기", modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }

        val before = textLayout(LONG_POT_TITLE).getLineEnd(0, visibleEnd = true)
        composeRule.runOnIdle { useNew = true }
        composeRule.waitForIdle()
        val after = textLayout(LONG_POT_TITLE)
        assertEquals("제목 줄 수", 1, after.lineCount)
        val afterCount = after.getLineEnd(0, visibleEnd = true)
        assertTrue("보이는 제목 글자 수: 전 $before, 후 $afterCount", afterCount > before)
    }

    private companion object {
        // "[태그] 화분 이름" — 화분 이름은 PotNameRule.MAX_LENGTH(15자), BackTopBarLayoutTest와 같은 값
        const val LONG_POT_TITLE = "[자격증] 정보처리기사실기대비하는새화분"
    }
}
