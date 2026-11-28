package com.a32b.plant.presentation.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.home.viewmodel.AttendanceUiState
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 출석판 다음 출석 칸 원의 semantics에 "다음 출석" 상태가 붙는지 확인 (#804)
 *
 * - 다음 칸은 화면에서만 구분되므로(색·테두리·숫자 칸 글꼴) 원에 stateDescription을 둔다
 * - 출석 칸 수(count)에 따라 다음 칸이 숫자 칸·아이템 칸·골드 칸이 되고, 28칸을 다 채우면 다음 칸이 없다
 */
@RunWith(AndroidJUnit4::class)
class AttendanceNextCellSemanticsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val isNextCell = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "다음 출석")

    private fun nextCells(): List<SemanticsNode> =
        composeRule.onAllNodes(isNextCell).fetchSemanticsNodes()

    private fun SemanticsNode.texts(): List<String> =
        config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()

    private fun SemanticsNode.descriptions(): List<String> =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

    @Test
    fun nextCell_hasStateDescription_onlyOnNextCell() {
        var count by mutableIntStateOf(0)
        composeRule.setContent {
            PlantTheme {
                AttendanceDialogContent(uiState = AttendanceUiState(count = count, buttonText = "출석하기"))
            }
        }

        // 숫자 칸 (0칸 출석 → 1일)
        composeRule.runOnIdle { count = 0 }
        nextCells().let {
            assertEquals("count 0: 다음 칸 수", 1, it.size)
            assertEquals("count 0: 1일 숫자", listOf("1"), it.single().texts())
        }

        // 아이템 칸 (8칸 출석 → 9일 물)
        composeRule.runOnIdle { count = 8 }
        nextCells().let {
            assertEquals("count 8: 다음 칸 수", 1, it.size)
            assertEquals("count 8: 9일 아이콘 이름", listOf("물"), it.single().descriptions())
        }

        // 골드 칸 (13칸 출석 → 14일 200G): 원 아래 "200G" 글씨는 원 노드에 합쳐지지 않음
        composeRule.runOnIdle { count = 13 }
        nextCells().let {
            assertEquals("count 13: 다음 칸 수", 1, it.size)
            assertEquals("count 13: 14일 아이콘 이름", listOf("200G"), it.single().descriptions())
            assertFalse("count 13: 원 아래 골드 글씨가 합쳐짐", it.single().texts().contains("200G"))
        }

        // 28칸 모두 출석 → 다음 칸 없음
        composeRule.runOnIdle { count = 28 }
        assertEquals("count 28: 다음 칸 수", 0, nextCells().size)
    }
}
