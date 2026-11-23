package com.a32b.plant.presentation.studying.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** "같이 공부 중" 목록의 공부 시간 문구 (#649) */
class StudyingUserTimeTextTest {

    @Test
    fun 공부_시간이_1분_미만이면_방금_공부_시작() {
        assertEquals(" 방금 공부 시작!", studyingUserTimeText(0L))
        assertEquals(" 방금 공부 시작!", studyingUserTimeText(59_999L))
    }

    @Test
    fun 공부_시간이_1분_이상이면_요약_시간_규칙을_따른다() {
        assertEquals(" 1분 째 공부중!", studyingUserTimeText(60_000L))
        assertEquals(" 2시간 12분 째 공부중!", studyingUserTimeText(132 * 60_000L))
        assertEquals(" 1시간 째 공부중!", studyingUserTimeText(60 * 60_000L))
    }
}
