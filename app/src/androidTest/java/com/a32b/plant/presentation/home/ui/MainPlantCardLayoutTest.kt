package com.a32b.plant.presentation.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.presentation.theme.PlantTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 홈 메인카드 배치 확인 (#680)
 * - 카드가 늘어나면 버튼 묶음은 카드 바닥에 붙고, 이미지는 이미지 영역(이름 아래 ~ "오늘 학습 시간" 위) 가운데에 온다
 * - 카드를 최소 높이(minIntrinsicHeight)로 줘도 버튼이 카드 밖으로 잘리지 않는다
 * 실제 화면의 카드 높이는 HomeBodyLayout이 정하지만(private), 여기서는 카드 높이를 직접 정해 MainPlantCard만 확인한다
 */
@RunWith(AndroidJUnit4::class)
class MainPlantCardLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val pot = Pot.EMPTY.copy(id = "pot", tagName = "자격증", name = "정보처리기사", potLevel = 2)

    private fun setCard(hasNoPot: Boolean, compact: Boolean, cardHeight: Dp?) {
        composeRule.setContent {
            PlantTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .testTag(CARD_TAG)
                        .width(CARD_WIDTH)
                        // null: 카드 최소 높이 그대로 (HomeBodyLayout 4번, 바깥 스크롤 경우)
                        .then(if (cardHeight == null) Modifier.height(IntrinsicSize.Min) else Modifier.height(cardHeight))
                ) {
                    MainPlantCard(
                        displayPot = if (hasNoPot) Pot.EMPTY else pot,
                        hasNoPot = hasNoPot,
                        todayStudyTime = "1시간 20분",
                        levelUpProgress = 0.4f,
                        levelUpProgressText = "40% (2시간/5시간)",
                        levelUpReady = false,
                        isLoading = false,
                        onStartClick = {},
                        onLevelUpClick = {},
                        onChangeOrMakeClick = {},
                        onRecordClick = {},
                        compact = compact
                    )
                }
            }
        }
    }

    private fun bounds(text: String): DpRect = composeRule.onNodeWithText(text).getUnclippedBoundsInRoot()

    /**
     * 버튼 묶음 맨 아래("공부 기록")가 카드 바닥 여백(일반 14dp, 간격 모드 7dp)만 남기고 붙어 있는지
     * Material3 버튼은 보이는 높이 40dp를 최소 터치 영역 48dp 가운데에 두므로, 보이는 버튼 아래에 4dp가 더 있다
     */
    private fun assertButtonsAtBottom(compact: Boolean) {
        val card = composeRule.onNodeWithTag(CARD_TAG).getUnclippedBoundsInRoot()
        val record = bounds("공부 기록")
        val padding = if (compact) 7.dp else 14.dp
        assertEquals((card.bottom - padding - BUTTON_TOUCH_INSET).value, record.bottom.value, TOLERANCE_DP)
    }

    /**
     * 이미지가 이미지 영역(이름 아래 8dp ~ "오늘 학습 시간" 위 8dp) 세로 가운데에 있는지
     * [upShiftRatio]: 그리는 위치를 이미지 높이 대비 이만큼 위로 옮긴 경우 (로고는 원 중심을 맞추려고 6%, Logo.kt)
     * → getUnclippedBoundsInRoot는 graphicsLayer 이동이 반영된 위치를 돌려주므로 그만큼 위를 기대값으로 둔다
     */
    private fun assertImageCentered(imageDescription: String, nameText: String, compact: Boolean, upShiftRatio: Float = 0f) {
        val gap = if (compact) 4.dp else 8.dp
        val areaTop = bounds(nameText).bottom + gap
        val areaBottom = bounds("오늘 학습 시간").top - gap
        val image = composeRule.onNodeWithContentDescription(imageDescription).getUnclippedBoundsInRoot()
        val imageHeight = image.bottom.value - image.top.value
        val areaCenter = (areaTop.value + areaBottom.value) / 2
        val imageCenter = (image.top.value + image.bottom.value) / 2
        assertEquals(areaCenter - imageHeight * upShiftRatio, imageCenter, TOLERANCE_DP)
        // 이동 전 자리 기준으로 영역 안에 들어가는지
        val layoutTop = image.top.value + imageHeight * upShiftRatio
        assertTrue("이미지가 영역 안에 있어야 함", layoutTop >= areaTop.value - TOLERANCE_DP && layoutTop + imageHeight <= areaBottom.value + TOLERANCE_DP)
    }

    @Test
    fun 큰_카드_화분있음_버튼은_바닥_이미지는_영역_가운데() {
        setCard(hasNoPot = false, compact = false, cardHeight = LARGE_CARD_HEIGHT)
        assertButtonsAtBottom(compact = false)
        assertImageCentered("화분 성장 단계 : lv.2", "정보처리기사", compact = false)
    }

    @Test
    fun 큰_카드_화분없음_버튼은_바닥_로고는_영역_가운데() {
        setCard(hasNoPot = true, compact = false, cardHeight = LARGE_CARD_HEIGHT)
        assertButtonsAtBottom(compact = false)
        assertImageCentered("로고 사진", "공부 화분이 없습니다", compact = false, upShiftRatio = LOGO_UP_SHIFT_RATIO)
    }

    @Test
    fun 최소_높이_간격모드_화분있음_버튼이_잘리지_않음() {
        setCard(hasNoPot = false, compact = true, cardHeight = null)
        assertButtonsAtBottom(compact = true)
        assertImageCentered("화분 성장 단계 : lv.2", "정보처리기사", compact = true)
    }

    @Test
    fun 최소_높이_간격모드_화분없음_버튼이_잘리지_않음() {
        setCard(hasNoPot = true, compact = true, cardHeight = null)
        assertButtonsAtBottom(compact = true)
        assertImageCentered("로고 사진", "공부 화분이 없습니다", compact = true, upShiftRatio = LOGO_UP_SHIFT_RATIO)
    }

    private companion object {
        const val CARD_TAG = "main_plant_card"
        // 1080px ÷ 2.625(420dpi) ≈ 411dp 화면에서 좌우 여백 16dp씩 뺀 본문 폭 (기기 폭이 더 좁으면 기기 폭으로 줄어듦)
        val CARD_WIDTH = 379.dp
        // (최소 터치 영역 48dp - 버튼 높이 40dp) ÷ 2
        val BUTTON_TOUCH_INSET = 4.dp
        // 카드가 크게 늘어난 경우 (본문 720dp 상한에 가까운 높이로 임의 지정)
        val LARGE_CARD_HEIGHT = 560.dp
        // Logo.kt의 LOGO_CIRCLE_CENTER_OFFSET_RATIO와 같은 값 (private이라 따로 둠)
        const val LOGO_UP_SHIFT_RATIO = 0.06f
        // px 반올림 오차 허용
        const val TOLERANCE_DP = 1f
    }
}
