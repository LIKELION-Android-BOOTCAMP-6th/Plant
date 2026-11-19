package com.a32b.plant.data.source.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.a32b.plant.data.model.StudyingSessionDto
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 타이머 서비스의 주기 저장(updateTime)이 조건이 맞을 때만 학습 시간을 쓰는지 확인 (#668)
 * - 같은 세션·공부 중(STUDYING)·일시정지 아님일 때만 time을 바꾸고 나머지 값은 그대로 둔다
 * - 백업이 없거나, 종료(FINISHED)·일시정지·다른 세션이면 아무것도 쓰지 않는다 (새 백업을 만들지 않음)
 *
 * 앱과 같은 DataStore 파일(studying_local)을 쓰므로 기기의 학습 백업이 지워진다 — 테스트 전후로 비운다
 */
@RunWith(AndroidJUnit4::class)
class StudyingLocalDataSourceImplTest {

    private val dataSource = StudyingLocalDataSourceImpl(ApplicationProvider.getApplicationContext())

    private val studying = StudyingSessionDto(
        userId = "uid",
        tag = "tag",
        title = "title",
        potId = "potId",
        time = 10_000L,
        log = listOf("goal"),
        startedAt = 1_000L,
        isPaused = false,
        status = STUDYING,
        sessionId = "session",
        completed = listOf(true)
    )

    @Before
    fun setUp() = runBlocking { dataSource.clear() }

    @After
    fun tearDown() = runBlocking { dataSource.clear() }

    @Test
    fun updatesOnlyTime_whenSameStudyingSession() = runBlocking {
        dataSource.save(studying)

        dataSource.updateTime(sessionId = "session", time = 40_000L, studyingStatus = STUDYING)

        assertEquals(studying.copy(time = 40_000L), dataSource.read())
    }

    @Test
    fun doesNotCreateBackup_whenNoBackup() = runBlocking {
        dataSource.updateTime(sessionId = "session", time = 40_000L, studyingStatus = STUDYING)

        assertNull(dataSource.read())
    }

    @Test
    fun doesNotUpdate_whenFinished() = runBlocking {
        val finished = studying.copy(status = "FINISHED")
        dataSource.save(finished)

        dataSource.updateTime(sessionId = "session", time = 40_000L, studyingStatus = STUDYING)

        assertEquals(finished, dataSource.read())
    }

    @Test
    fun doesNotUpdate_whenPaused() = runBlocking {
        val paused = studying.copy(isPaused = true)
        dataSource.save(paused)

        dataSource.updateTime(sessionId = "session", time = 40_000L, studyingStatus = STUDYING)

        assertEquals(paused, dataSource.read())
    }

    @Test
    fun doesNotUpdate_whenOtherSession() = runBlocking {
        dataSource.save(studying)

        dataSource.updateTime(sessionId = "other", time = 40_000L, studyingStatus = STUDYING)

        assertEquals(studying, dataSource.read())
    }

    @Test
    fun doesNotUpdate_whenOldBackupWithoutSessionId() = runBlocking {
        val old = studying.copy(sessionId = null, status = null)
        dataSource.save(old)

        dataSource.updateTime(sessionId = "session", time = 40_000L, studyingStatus = STUDYING)

        assertEquals(old, dataSource.read())
    }

    private companion object {
        const val STUDYING = "STUDYING"
    }
}
