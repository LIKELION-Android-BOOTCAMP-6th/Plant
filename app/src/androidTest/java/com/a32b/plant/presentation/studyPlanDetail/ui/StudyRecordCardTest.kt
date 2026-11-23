package com.a32b.plant.presentation.studyPlanDetail.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.captureToImage
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.presentation.theme.DarkColorScheme
import com.a32b.plant.presentation.theme.LightColorScheme
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 공부 기록 카드 [StudyRecordCard] 정보 배치 (#682)
 *
 * - 1줄 제목, 2줄 "공부 시간 HH:MM:SS". 제목에 이미 날짜가 있어 2줄에 날짜를 다시 쓰지 않음
 * - 제목이 없으면 날짜가 제목 (날짜는 한 번만), createAt도 없으면 "-"
 * - 학습 내용은 2개까지, 넘치면 "외 N개"
 * - 공유 모드: 체크박스 표시, 휴지통 숨김
 * - 카드 배경: primaryContainer, 다크에서 배경(background)과 다른 색
 */
@RunWith(AndroidJUnit4::class)
class StudyRecordCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val createAt = 1_763_900_000_000L // 2025-11-23
    private val dateText = TimeFormatter.formatTimeToDate(createAt)
    private val studyingTime = 3_723_000L // 1시간 2분 3초
    private val timeText = "공부 시간 ${TimeFormatter.formatToDigitalClock(studyingTime)}"

    private fun log(
        title: String = "영단어 외우기",
        contents: List<String> = emptyList(),
        createAt: Long? = this.createAt
    ) = StudyLog(
        title = title,
        contents = contents,
        completedContents = contents.map { false },
        studyingTime = studyingTime,
        createAt = createAt,
        startedAt = 0L,
        id = "log1",
        isSelected = false
    )

    private fun setCard(
        log: StudyLog,
        isShareMode: Boolean = false,
        darkTheme: Boolean = false,
        onCardClick: () -> Unit = {},
        onDeleteClick: () -> Unit = {}
    ) {
        composeRule.setContent {
            PlantTheme(darkTheme = darkTheme) {
                StudyRecordCard(
                    log = log,
                    isShareMode = isShareMode,
                    onSelectionChange = {},
                    onCardClick = onCardClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun withTitle_showsTitleAndTimeWithoutDate() {
        setCard(log())
        composeRule.onNodeWithText("영단어 외우기").assertIsDisplayed()
        composeRule.onNodeWithText(timeText).assertIsDisplayed()
        composeRule.onAllNodesWithText(dateText, substring = true).assertCountEquals(0)
    }

    @Test
    fun withoutTitle_dateBecomesTitleAndIsNotRepeated() {
        setCard(log(title = ""))
        composeRule.onAllNodesWithText(dateText, substring = true).assertCountEquals(1)
        composeRule.onNodeWithText(dateText).assertIsDisplayed()
        composeRule.onNodeWithText(timeText).assertIsDisplayed()
    }

    @Test
    fun noTitleAndNullCreateAt_showsDashAsTitle() {
        setCard(log(title = "", createAt = null))
        composeRule.onNodeWithText("-").assertIsDisplayed()
        composeRule.onNodeWithText(timeText).assertIsDisplayed()
    }

    @Test
    fun noContents_showsNoRestCount() {
        setCard(log(contents = emptyList()))
        composeRule.onAllNodesWithText("외 ", substring = true).assertCountEquals(0)
    }

    @Test
    fun twoContents_showsBothWithoutRestCount() {
        setCard(log(contents = listOf("단어 1", "단어 2")))
        composeRule.onNodeWithText("단어 1").assertIsDisplayed()
        composeRule.onNodeWithText("단어 2").assertIsDisplayed()
        composeRule.onAllNodesWithText("외 ", substring = true).assertCountEquals(0)
    }

    @Test
    fun threeOrMoreContents_showsTwoAndRestCount() {
        setCard(log(contents = listOf("단어 1", "단어 2", "단어 3", "단어 4")))
        composeRule.onNodeWithText("단어 1").assertIsDisplayed()
        composeRule.onNodeWithText("단어 2").assertIsDisplayed()
        composeRule.onAllNodesWithText("단어 3").assertCountEquals(0)
        composeRule.onNodeWithText("외 2개").assertIsDisplayed()
    }

    @Test
    fun normalMode_showsTrashWithoutCheckbox_andClicksWork() {
        var cardClicks = 0
        var deleteClicks = 0
        setCard(log(), onCardClick = { cardClicks++ }, onDeleteClick = { deleteClicks++ })
        composeRule.onAllNodes(isToggleable()).assertCountEquals(0)

        composeRule.onNodeWithContentDescription("상세 공부 기록 삭제").performClick()
        assertEquals(1, deleteClicks)
        assertEquals("휴지통을 눌러도 카드 클릭은 호출되지 않아야 함", 0, cardClicks)

        composeRule.onNodeWithText("영단어 외우기").performClick()
        assertEquals(1, cardClicks)
    }

    @Test
    fun shareMode_showsCheckboxAndHidesTrash() {
        setCard(log(), isShareMode = true)
        composeRule.onAllNodes(isToggleable()).assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription("상세 공부 기록 삭제").assertCountEquals(0)
    }

    // 카드 왼쪽 안쪽 여백(16dp) 세로 가운데 픽셀 = 카드 배경색
    private fun cardBackgroundArgb(darkTheme: Boolean): Int {
        setCard(log(contents = listOf("단어 1")), darkTheme = darkTheme)
        // 클릭 가능한 노드 중 첫 번째가 카드(그 안의 휴지통 버튼은 두 번째)
        val pixels = composeRule.onAllNodes(hasClickAction()).onFirst()
            .captureToImage()
            .toPixelMap()
        return pixels[8, pixels.height / 2].toArgb()
    }

    @Test
    fun darkTheme_cardDiffersFromBackground() {
        val card = cardBackgroundArgb(darkTheme = true)
        assertEquals(DarkColorScheme.primaryContainer.toArgb(), card)
        assertNotEquals(DarkColorScheme.background.toArgb(), card)
    }

    @Test
    fun lightTheme_cardUsesPrimaryContainer() {
        assertEquals(LightColorScheme.primaryContainer.toArgb(), cardBackgroundArgb(darkTheme = false))
    }
}
