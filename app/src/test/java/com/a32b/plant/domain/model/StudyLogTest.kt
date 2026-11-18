package com.a32b.plant.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 학습 기록 저장 가능 기간 판단 (#633). 보안 규칙 isValidNewStudyLog()의
 * `startedAt >= request.time - maxOfflineSyncDays()` 와 같은 기준인지 확인한다.
 */
class StudyLogTest {

    private val day = 24 * 60 * 60 * 1000L
    private val now = 1_800_000_000_000L

    @Test
    fun `started just now or within 7 days - can be saved`() {
        assertFalse(StudyLog.isTooOldToSave(startedAt = now, nowMillis = now))
        assertFalse(StudyLog.isTooOldToSave(startedAt = now - 3 * day, nowMillis = now))
    }

    @Test
    fun `started exactly 7 days ago - can be saved like the rule (greater or equal)`() {
        assertFalse(StudyLog.isTooOldToSave(startedAt = now - StudyLog.MAX_OFFLINE_SYNC_DAYS * day, nowMillis = now))
    }

    @Test
    fun `started more than 7 days ago - too old`() {
        assertTrue(StudyLog.isTooOldToSave(startedAt = now - StudyLog.MAX_OFFLINE_SYNC_DAYS * day - 1, nowMillis = now))
    }

    @Test
    fun `started in 1970 - too old`() {
        assertTrue(StudyLog.isTooOldToSave(startedAt = 0L, nowMillis = now))
    }
}
