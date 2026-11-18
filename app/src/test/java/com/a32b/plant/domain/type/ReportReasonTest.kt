package com.a32b.plant.domain.type

import org.junit.Assert.assertEquals
import org.junit.Test

class ReportReasonTest {

    @Test
    fun `저장 키는 기존 값을 유지하고 copyright가 추가된다`() {
        assertEquals(
            listOf("abuse", "spam", "inappropriate", "copyright", "other"),
            ReportReason.entries.map { it.firestoreKey }
        )
    }

    @Test
    fun `기타는 신고 사유 목록의 마지막에 있다`() {
        assertEquals(ReportReason.OTHER, ReportReason.entries.last())
    }

    @Test
    fun `상세 입력란은 권리 침해와 기타에서만 열린다`() {
        assertEquals(
            setOf(ReportReason.COPYRIGHT, ReportReason.OTHER),
            ReportReason.entries.filter { it.allowsDetail }.toSet()
        )
    }
}
