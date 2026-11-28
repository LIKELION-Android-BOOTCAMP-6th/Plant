package com.a32b.plant.domain.type

import org.junit.Assert.assertEquals
import org.junit.Test

/** 기기에 저장된 정렬 이름 되돌리기 (#803) */
class PotSortTypeTest {

    @Test
    fun `저장된 이름은 같은 정렬로 되돌린다`() {
        PotSortType.entries.forEach { sortType ->
            assertEquals(sortType, PotSortType.fromName(sortType.name))
        }
    }

    @Test
    fun `저장된 값이 없으면 생성일`() {
        assertEquals(PotSortType.CREATED_DATE, PotSortType.fromName(null))
    }

    @Test
    fun `지금 항목에 없는 이름이면 생성일`() {
        assertEquals(PotSortType.CREATED_DATE, PotSortType.fromName("STUDY_COUNT"))
        assertEquals(PotSortType.CREATED_DATE, PotSortType.fromName(""))
        // 화면 이름(displayName)이나 대소문자가 다른 이름은 저장 형식이 아니다
        assertEquals(PotSortType.CREATED_DATE, PotSortType.fromName("누적 학습 시간"))
        assertEquals(PotSortType.CREATED_DATE, PotSortType.fromName("level"))
    }
}
