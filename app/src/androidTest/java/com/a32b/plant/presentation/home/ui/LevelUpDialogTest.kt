package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.home.viewmodel.LevelUpDialogUiState
import com.a32b.plant.presentation.home.viewmodel.LevelUpItemRequirement
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 레벨업 확인 다이얼로그 (#678)
 * - 버튼: 조건 부족이어도 눌리고(비활성 버튼 + Toast 패턴) TalkBack 상태를 알림, 처리 중에는 버튼·취소를 막음
 * - 큰 글꼴: 본문(LevelUpDialogContent)의 글자가 한 줄을 넘지 않고 이름과 개수가 겹치지 않음
 *   Dialog 안에는 LocalDensity 덮어쓰기가 닿지 않아 본문만 직접 그린다
 *   폭 232dp: M3 AlertDialog 최소 폭 280dp(material3 1.4.0 AlertDialog.kt DialogMinWidth) - 좌우 여백 24dp씩
 *   폭 272dp: 테스트 기기(SM-N986N, 420dpi)의 uiautomator 다이얼로그 bounds 폭 840px ÷ 2.625 = 320dp - 48dp
 */
@RunWith(AndroidJUnit4::class)
class LevelUpDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val notReadyState = LevelUpDialogUiState(
        potId = "pot-1",
        currentLevel = 1,
        items = listOf(
            LevelUpItemRequirement(ItemType.HEART, owned = 6, required = 5),
            LevelUpItemRequirement(ItemType.SUN, owned = 1, required = 3)
        ),
        currentStudyTimeText = "5시간",
        requiredStudyTimeText = "5시간",
        remainingStudyTimeText = null
    )

    // 가장 긴 경우: Lv.5 → 6 재료 5종이 모두 부족, 학습 시간 150시간
    private val longestState = LevelUpDialogUiState(
        potId = "pot-1",
        currentLevel = 5,
        items = listOf(
            LevelUpItemRequirement(ItemType.HEART, owned = 0, required = 50),
            LevelUpItemRequirement(ItemType.SUN, owned = 0, required = 30),
            LevelUpItemRequirement(ItemType.WATER, owned = 0, required = 35),
            LevelUpItemRequirement(ItemType.FERTILIZER, owned = 0, required = 10),
            // 보유가 세 자리인 경우
            LevelUpItemRequirement(ItemType.NUTRIENT, owned = 999, required = 5)
        ),
        currentStudyTimeText = "149시간 59분",
        requiredStudyTimeText = "150시간",
        remainingStudyTimeText = "1분"
    )

    @Test
    fun notReadyButtonStillClickableAndAnnouncesState() {
        var confirms = 0
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                LevelUpDialog(state = notReadyState, isLoading = false, onConfirm = { confirms++ }, onDismiss = {})
            }
        }

        val button = composeRule.onNodeWithText("레벨업")
        assertEquals(LEVEL_UP_NOT_READY_STATE_DESCRIPTION, button.stateDescription())
        button.assertIsEnabled().performClick()
        assertEquals(1, confirms)
        composeRule.onNodeWithText("2개 부족", useUnmergedTree = true).assertExists()
    }

    @Test
    fun readyButtonHasNoNotReadyState() {
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                LevelUpDialog(
                    state = notReadyState.copy(items = notReadyState.items.map { it.copy(owned = it.required) }),
                    isLoading = false,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        assertNull(composeRule.onNodeWithText("레벨업").stateDescription())
    }

    @Test
    fun loadingBlocksConfirmAndCancel() {
        var dismisses = 0
        composeRule.setContent {
            PlantTheme(darkTheme = true) {
                LevelUpDialog(state = notReadyState, isLoading = true, onConfirm = {}, onDismiss = { dismisses++ })
            }
        }

        composeRule.onNodeWithText("레벨업 중...").assertIsNotEnabled()
        composeRule.onNodeWithText("취소").assertIsNotEnabled()
        assertEquals(0, dismisses)
    }

    @Test
    fun contentTextsStayOnOneLineWithoutOverlapInLargeFont() {
        var combo by mutableStateOf(WIDTHS.first() to FONT_SCALES.first())
        composeRule.setContent {
            key(combo) { Frame(combo.first, combo.second) }
        }
        val failures = mutableListOf<String>()
        for (width in WIDTHS) for (fontScale in FONT_SCALES) {
            composeRule.runOnIdle { combo = width to fontScale }
            composeRule.waitForIdle()
            val where = "폭 $width, 글꼴 $fontScale"

            for (item in longestState.items) {
                val name = composeRule.onNodeWithText(item.type.kor, useUnmergedTree = true)
                val count = composeRule.onNodeWithText("${item.owned} / ${item.required}개", useUnmergedTree = true)
                listOf(name, count).forEach { node ->
                    val lines = node.textLayout().lineCount
                    if (lines != 1) failures += "$where: '${node.text()}' ${lines}줄"
                }
                if (!item.isEnough) {
                    val lacking = composeRule.onNodeWithText("${item.lackingCount}개 부족", useUnmergedTree = true)
                    val lines = lacking.textLayout().lineCount
                    if (lines != 1) failures += "$where: '${lacking.text()}' ${lines}줄"
                }
                val nameRight = name.fetchSemanticsNode().boundsInRoot.right
                val countLeft = count.fetchSemanticsNode().boundsInRoot.left
                if (nameRight > countLeft) failures += "$where: '${item.type.kor}'와 개수 겹침 ($nameRight > $countLeft)"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Composable
    private fun Frame(width: Dp, fontScale: Float) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
            PlantTheme(darkTheme = false) {
                Box(modifier = Modifier.requiredWidth(width)) {
                    LevelUpDialogContent(longestState)
                }
            }
        }
    }

    private fun SemanticsNodeInteraction.stateDescription(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun SemanticsNodeInteraction.text(): String =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString().orEmpty()

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val action = fetchSemanticsNode().config.getOrNull(SemanticsActions.GetTextLayoutResult)
        assertFalse("글자 배치 결과를 얻을 수 없음", action == null)
        action!!.action!!.invoke(results)
        return results.single()
    }

    private companion object {
        val WIDTHS = listOf(232.dp, 272.dp)
        val FONT_SCALES = listOf(1.0f, 1.3f, 1.5f, 2.0f)
    }
}
