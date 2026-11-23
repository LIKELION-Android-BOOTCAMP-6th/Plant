package com.a32b.plant.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** 시간 표기 규칙 (#649): 흘러가는 시간은 HH:mm:ss 고정, 누적·요약 시간은 "n시간 n분" */
class TimeFormatterTest {

    @Test
    fun 흘러가는_시간은_1시간_미만도_시_자리를_포함한다() {
        assertEquals("00:00:00", TimeFormatter.formatToDigitalClock(0L))
        assertEquals("00:00:59", TimeFormatter.formatToDigitalClock(59 * SECOND))
        assertEquals("00:01:00", TimeFormatter.formatToDigitalClock(MINUTE))
        assertEquals("00:59:00", TimeFormatter.formatToDigitalClock(59 * MINUTE))
        assertEquals("00:47:12", TimeFormatter.formatToDigitalClock(47 * MINUTE + 12 * SECOND))
        assertEquals("01:00:00", TimeFormatter.formatToDigitalClock(HOUR))
        assertEquals("25:00:00", TimeFormatter.formatToDigitalClock(25 * HOUR))
    }

    @Test
    fun 흘러가는_시간은_1초_미만을_버린다() {
        assertEquals("00:00:00", TimeFormatter.formatToDigitalClock(999L))
    }

    @Test
    fun 요약_시간_0은_0분() {
        assertEquals("0분", TimeFormatter.formatToHoursMinutes(0L))
    }

    @Test
    fun 요약_시간_1초_이상_1분_미만은_1분_미만() {
        assertEquals("1분 미만", TimeFormatter.formatToHoursMinutes(1L))
        assertEquals("1분 미만", TimeFormatter.formatToHoursMinutes(59 * SECOND))
        assertEquals("1분 미만", TimeFormatter.formatToHoursMinutes(MINUTE - 1))
    }

    @Test
    fun 요약_시간은_초를_버린다() {
        assertEquals("1분", TimeFormatter.formatToHoursMinutes(MINUTE))
        assertEquals("1분", TimeFormatter.formatToHoursMinutes(MINUTE + 59 * SECOND))
        assertEquals("59분", TimeFormatter.formatToHoursMinutes(59 * MINUTE))
        assertEquals("2시간 25분", TimeFormatter.formatToHoursMinutes(2 * HOUR + 25 * MINUTE + 59 * SECOND))
    }

    @Test
    fun 요약_시간에서_분이_0이면_시간만_쓴다() {
        assertEquals("1시간", TimeFormatter.formatToHoursMinutes(HOUR))
        assertEquals("1시간", TimeFormatter.formatToHoursMinutes(HOUR + 59 * SECOND))
    }

    @Test
    fun 요약_시간은_24시간을_넘어도_일_단위로_넘기지_않는다() {
        assertEquals("25시간", TimeFormatter.formatToHoursMinutes(25 * HOUR))
        assertEquals("38시간 25분", TimeFormatter.formatToHoursMinutes(38 * HOUR + 25 * MINUTE))
    }

    @Test
    fun 요약_시간_음수는_0분() {
        assertEquals("0분", TimeFormatter.formatToHoursMinutes(-MINUTE))
    }

    private companion object {
        const val SECOND = 1_000L
        const val MINUTE = 60 * SECOND
        const val HOUR = 60 * MINUTE
    }
}
