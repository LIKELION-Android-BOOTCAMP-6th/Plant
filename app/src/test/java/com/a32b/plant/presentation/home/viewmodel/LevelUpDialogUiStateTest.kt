package com.a32b.plant.presentation.home.viewmodel

import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.type.ItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 레벨업 확인 다이얼로그 상태·문구 (#678) */
class LevelUpDialogUiStateTest {

    private fun pot(level: Int, studyMs: Long, isCompleted: Boolean = false) =
        Pot.EMPTY.copy(id = "pot1", potLevel = level, potTotalStudyingTime = studyMs, isCompleted = isCompleted)

    private fun item(heart: Int = 0, sun: Int = 0, water: Int = 0, fertilizer: Int = 0, nutrient: Int = 0) =
        Item(heart = heart, sun = sun, water = water, fertilizer = fertilizer, nutrient = nutrient, box = 0)

    // Lv.1 → 2 조건: 5시간, 햇빛 3·물 1·정성 5
    @Test
    fun 조건_충족이면_ready이고_안내_문구가_없다() {
        val state = buildLevelUpDialogUiState(pot(1, 5 * HOUR), item(heart = 5, sun = 3, water = 1))!!

        assertEquals("pot1", state.potId)
        assertEquals(2, state.newLevel)
        assertTrue(state.isReady)
        assertNull(state.remainingStudyTimeText)
        assertNull(state.notReadyMessage)
        assertEquals("5시간", state.currentStudyTimeText)
        assertEquals("5시간", state.requiredStudyTimeText)
    }

    @Test
    fun 재료는_홈_아이템_칸_순서로_보유와_필요_개수를_담는다() {
        val state = buildLevelUpDialogUiState(pot(1, 5 * HOUR), item(heart = 7, sun = 1))!!

        assertEquals(
            listOf(
                LevelUpItemRequirement(ItemType.HEART, owned = 7, required = 5),
                LevelUpItemRequirement(ItemType.SUN, owned = 1, required = 3),
                LevelUpItemRequirement(ItemType.WATER, owned = 0, required = 1)
            ),
            state.items
        )
    }

    @Test
    fun 재료만_부족하면_부족한_개수를_안내한다() {
        val state = buildLevelUpDialogUiState(pot(1, 5 * HOUR), item(heart = 5, sun = 1))!!

        assertFalse(state.isReady)
        assertEquals("재료가 부족해요: 햇빛 2개, 물 1개", state.notReadyMessage)
    }

    @Test
    fun 학습_시간이_부족하면_남은_시간을_분_단위로_올려_안내한다() {
        // 4시간 59분 30초: 30초 남음 → "1분"
        val state = buildLevelUpDialogUiState(
            pot(1, 5 * HOUR - 30_000L),
            item(heart = 5, sun = 3, water = 1)
        )!!

        assertFalse(state.isReady)
        assertEquals("4시간 59분", state.currentStudyTimeText)
        assertEquals("1분", state.remainingStudyTimeText)
        assertEquals("학습 시간이 1분 더 필요해요.", state.notReadyMessage)
    }

    @Test
    fun 학습_시간과_재료가_모두_부족하면_학습_시간을_먼저_안내한다() {
        val state = buildLevelUpDialogUiState(pot(1, 3 * HOUR + 20 * MINUTE), item())!!

        assertEquals("1시간 40분", state.remainingStudyTimeText)
        assertEquals("학습 시간이 1시간 40분 더 필요해요.", state.notReadyMessage)
    }

    @Test
    fun 학습_시간_0이면_0분으로_표시한다() {
        val state = buildLevelUpDialogUiState(pot(0, 0L), item(heart = 1))!!

        assertEquals("0분", state.currentStudyTimeText)
        assertEquals("1시간", state.requiredStudyTimeText)
        assertEquals("1시간", state.remainingStudyTimeText)
    }

    @Test
    fun 최고_레벨과_완료_화분은_다이얼로그를_만들지_않는다() {
        assertNull(buildLevelUpDialogUiState(pot(PotLevelConditions.MAX_LEVEL, 1_000 * HOUR), item()))
        assertNull(buildLevelUpDialogUiState(pot(1, 5 * HOUR, isCompleted = true), item(heart = 5, sun = 3, water = 1)))
    }

    @Test
    fun 성공_문구는_새_레벨과_조사를_맞춘다() {
        assertEquals("Lv.1로 성장했어요!", levelUpSuccessMessage(1, isPending = false))
        assertEquals("Lv.2로 성장했어요!", levelUpSuccessMessage(2, isPending = false))
        assertEquals("Lv.3으로 성장했어요!", levelUpSuccessMessage(3, isPending = false))
        assertEquals("Lv.4로 성장했어요!", levelUpSuccessMessage(4, isPending = false))
        assertEquals("Lv.5로 성장했어요!", levelUpSuccessMessage(5, isPending = false))
        assertEquals("Lv.6으로 성장했어요!", levelUpSuccessMessage(6, isPending = false))
    }

    @Test
    fun 오프라인_성공_문구는_자동_저장_안내를_붙인다() {
        assertEquals(
            "Lv.2로 성장했어요! 네트워크에 연결되면 자동으로 저장돼요.",
            HomeEvent.LevelUpSuccess(newLevel = 2, isPending = true).message
        )
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
    }
}
