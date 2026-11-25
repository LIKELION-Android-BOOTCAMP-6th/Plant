package com.a32b.plant.presentation.home.viewmodel

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.usecase.pot.CheckLevelUpConditionUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 홈 레벨업 진행 바: 레벨마다 직전 기준 0% ~ 이번 기준 100% (#679) */
class LevelUpProgressTest {

    private val checkLevelUpCondition = CheckLevelUpConditionUseCase()

    private fun pot(level: Int, studyMs: Long) =
        Pot.EMPTY.copy(id = "pot1", potLevel = level, potTotalStudyingTime = studyMs)

    @Test
    fun 레벨마다_0퍼센트_기준은_직전_레벨의_레벨업_기준_시간이다() {
        // 이슈 표: Lv0 0~1, Lv1 1~5, Lv2 5~15, Lv3 15~30, Lv4 30~70, Lv5 70~150 (시간)
        val expected = listOf(0L to 1L, 1L to 5L, 5L to 15L, 15L to 30L, 30L to 70L, 70L to 150L)
        expected.forEachIndexed { level, (start, end) ->
            assertEquals("Lv$level 시작", start, PotLevelConditions.getProgressStartHours(level))
            assertEquals("Lv$level 끝", end, PotLevelConditions.getCondition(level)!!.minHoursForLevelUp)
        }
    }

    @Test
    fun 모든_레벨에서_구간_길이가_0보다_크다() {
        PotLevelConditions.table.keys.forEach { level ->
            val start = PotLevelConditions.getProgressStartHours(level)!!
            val end = PotLevelConditions.getCondition(level)!!.minHoursForLevelUp
            assertTrue("Lv$level: $start < $end", start < end)
        }
    }

    @Test
    fun 누적이_직전_기준과_같으면_0퍼센트() {
        val result = buildLevelUpProgress(pot(1, 1 * HOUR))

        assertEquals(0f, result.progress, DELTA)
        assertEquals("0% (0분/4시간)", result.text)
    }

    @Test
    fun 구간_중간이면_구간_기준으로_계산한다() {
        // Lv1 구간 1~5시간, 누적 2시간 48분 → 1시간 48분/4시간 = 45%
        val result = buildLevelUpProgress(pot(1, 2 * HOUR + 48 * MINUTE))

        assertEquals(0.45f, result.progress, DELTA)
        assertEquals("45% (1시간 48분/4시간)", result.text)
    }

    @Test
    fun 모든_레벨에서_100퍼센트와_레벨업_가능_시점이_일치한다() {
        PotLevelConditions.table.forEach { (level, condition) ->
            val requiredMs = condition.minHoursForLevelUp * HOUR

            val atRequired = buildLevelUpProgress(pot(level, requiredMs))
            assertEquals("Lv$level 기준 도달", 1f, atRequired.progress, DELTA)
            assertTrue("Lv$level 기준 도달 문구", atRequired.text.startsWith("100% "))
            assertTrue("Lv$level 기준 도달 레벨업", checkLevelUpCondition(level, requiredMs))

            // 1ms 모자라면 레벨업 불가 → 문구가 100%로 보이지 않는다 (퍼센트 버림)
            // (바 값은 Float 정밀도 때문에 1f로 반올림될 수 있어 문구로 확인한다)
            val justBefore = buildLevelUpProgress(pot(level, requiredMs - 1))
            assertTrue("Lv$level 직전 문구", justBefore.text.startsWith("99% "))
            assertFalse("Lv$level 직전 레벨업", checkLevelUpCondition(level, requiredMs - 1))
        }
    }

    @Test
    fun 늦게_레벨업하면_0퍼센트보다_큰_값에서_시작한다() {
        // Lv0에서 3시간 채우고 Lv1로 → (3-1)/(5-1) = 50%
        val result = buildLevelUpProgress(pot(1, 3 * HOUR))

        assertEquals(0.5f, result.progress, DELTA)
        assertEquals("50% (2시간/4시간)", result.text)
    }

    @Test
    fun 레벨업을_미뤄_기준을_넘으면_100퍼센트로_자른다() {
        val result = buildLevelUpProgress(pot(1, 7 * HOUR))

        assertEquals(1f, result.progress, DELTA)
        assertEquals("100% (4시간/4시간)", result.text)
    }

    @Test
    fun 기록_삭제로_누적이_직전_기준_아래면_0퍼센트() {
        // Lv2 구간 5~15시간, 기록 삭제로 누적 3시간
        val result = buildLevelUpProgress(pot(2, 3 * HOUR))

        assertEquals(0f, result.progress, DELTA)
        assertEquals("0% (0분/10시간)", result.text)
    }

    @Test
    fun 최고_레벨은_100퍼센트와_최고_레벨_문구() {
        val maxLevel = PotLevelConditions.MAX_LEVEL
        assertNull(PotLevelConditions.getProgressStartHours(maxLevel))

        val result = buildLevelUpProgress(pot(maxLevel, 500 * HOUR))

        assertEquals(1f, result.progress, DELTA)
        assertEquals(MAX_LEVEL_PROGRESS_TEXT, result.text)
    }

    @Test
    fun Lv0은_0시간부터_시작한다() {
        assertEquals(0L, PotLevelConditions.getProgressStartHours(0))

        val empty = buildLevelUpProgress(pot(0, 0L))
        assertEquals(0f, empty.progress, DELTA)
        assertEquals("0% (0분/1시간)", empty.text)

        val half = buildLevelUpProgress(pot(0, 30 * MINUTE))
        assertEquals(0.5f, half.progress, DELTA)
        assertEquals("50% (30분/1시간)", half.text)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DELTA = 0.0001f
    }
}
