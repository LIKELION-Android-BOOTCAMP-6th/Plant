package com.a32b.plant.presentation.community.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 글쓰기·버그 제보·개선 제안 공통 입력란 PostInputField 확인 (#671)
 * - 처음 값(수정·공유 모드에서 ViewModel이 채운 값)을 보여 준다
 * - 입력하면 바뀐 전체 문자열을 onValueChange로 알린다
 * - 글자 수를 넘는 입력은 통째로 되돌리고 onValueChange도 부르지 않는다
 * - 바깥 세로 스크롤 안에서 줄을 계속 추가하면 커서를 따라 바깥이 스크롤된다
 *   (value: String 방식 TextField로 되돌리면 이 테스트가 실패한다)
 */
@RunWith(AndroidJUnit4::class)
class PostInputFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val changes = mutableListOf<String>()

    private fun setField(initial: String?, maxLength: Int = 1000, scrollState: ScrollState? = null) {
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                val outerScroll = scrollState ?: rememberScrollState()
                Column(
                    modifier = Modifier
                        .width(360.dp)
                        .height(VIEWPORT_HEIGHT)
                        .verticalScroll(outerScroll)
                ) {
                    PostInputField(
                        value = initial,
                        onValueChange = { changes += it },
                        placeholder = PLACEHOLDER,
                        maxLength = maxLength
                    )
                    // 입력란 아래에 내용이 더 있어도 스크롤 기준이 커서인지 보기 위한 여백
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }
        }
    }

    @Test
    fun initialValue_isShownWithCount() {
        setField(initial = "기존 본문")

        composeRule.onNodeWithText("기존 본문").assertExists()
        composeRule.onNodeWithText("5 / 1000").assertExists()
    }

    @Test
    fun nullInitialValue_showsPlaceholderAndZeroCount() {
        setField(initial = null)

        composeRule.onNodeWithText(PLACEHOLDER).assertExists()
        composeRule.onNodeWithText("0 / 1000").assertExists()
    }

    @Test
    fun typing_reportsWholeText() {
        setField(initial = "가")

        composeRule.onNodeWithText("가").performClick()
        composeRule.onNodeWithText("가").performTextInput("나다")
        composeRule.waitForIdle()

        assertEquals("가나다", changes.last())
        composeRule.onNodeWithText("3 / 1000").assertExists()
    }

    @Test
    fun overMaxLength_isRevertedAndNotReported() {
        setField(initial = "abc", maxLength = 5)

        composeRule.onNodeWithText("abc").performClick()
        composeRule.onNodeWithText("abc").performTextInput("def") // 6자가 되므로 거부
        composeRule.waitForIdle()

        composeRule.onNodeWithText("abc").assertExists()
        composeRule.onNodeWithText("3 / 5").assertExists()
        assertTrue("거부된 입력이 전달됨: $changes", changes.none { it.length > 5 })
    }

    @Test
    fun addingLines_scrollsOuterContainerToCursor() {
        val scrollState = ScrollState(0)
        setField(initial = "첫 줄", scrollState = scrollState)

        composeRule.onNodeWithText("첫 줄").performClick()
        composeRule.waitForIdle()
        assertEquals(0, scrollState.value)

        // 한 줄씩 넣어 커서가 viewport(VIEWPORT_HEIGHT) 아래로 내려가게 한다
        repeat(LINES_TO_ADD) { i ->
            composeRule.onNodeWithText("첫 줄", substring = true).performTextInput("\n줄 $i")
            composeRule.waitForIdle()
        }

        assertTrue("커서를 따라 바깥이 스크롤되지 않음: ${scrollState.value}", scrollState.value > 0)
    }

    private companion object {
        const val PLACEHOLDER = "내용을 입력하세요"
        val VIEWPORT_HEIGHT = 300.dp
        const val LINES_TO_ADD = 20
    }
}
