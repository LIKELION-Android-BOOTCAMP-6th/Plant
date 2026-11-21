package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.usecase.studying.ObserveStudyingUsersUseCase.Companion.REFILTER_INTERVAL_MS
import com.a32b.plant.domain.usecase.studying.ObserveStudyingUsersUseCase.Companion.STALE_THRESHOLD_MS
import com.a32b.plant.fake.FakeStudyingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 공부 중 목록에서 유령(강제 종료로 남은 문서) 숨김 (#618)
 * - updatedAt이 기준 시간보다 오래됐거나 없으면 빼고, 남은 사람 중 앞에서 3명만 보낸다
 * - 리스너가 다시 불리지 않아도 주기마다 다시 걸러 유령이 사라진다
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveStudyingUsersUseCaseTest {

    private val now = 1_000_000_000L

    private fun user(uid: String, updatedAt: Long?, studyingTime: Long = 0L) =
        StudyingUser(uid, "nick-$uid", "", "tag", studyingTime, updatedAt)

    private fun uids(users: List<StudyingUser>) = users.map { it.uid }

    // --- 거르기 (filterActive) ---

    @Test
    fun `keeps users updated within threshold`() {
        val users = listOf(user("a", now), user("b", now - STALE_THRESHOLD_MS + 1))
        assertEquals(listOf("a", "b"), uids(ObserveStudyingUsersUseCase.filterActive(users, now)))
    }

    @Test
    fun `drops users at or over threshold`() {
        val users = listOf(user("a", now - STALE_THRESHOLD_MS), user("b", now - 20 * 60 * 1000L))
        assertEquals(emptyList<String>(), uids(ObserveStudyingUsersUseCase.filterActive(users, now)))
    }

    @Test
    fun `drops users without updatedAt`() {
        val users = listOf(user("a", null), user("b", now))
        assertEquals(listOf("b"), uids(ObserveStudyingUsersUseCase.filterActive(users, now)))
    }

    @Test
    fun `keeps users whose updatedAt is in the future (device clock behind server)`() {
        val users = listOf(user("a", now + 5 * 60 * 1000L))
        assertEquals(listOf("a"), uids(ObserveStudyingUsersUseCase.filterActive(users, now)))
    }

    @Test
    fun `ghost in top 3 is replaced by next active user and order is kept`() {
        // 쿼리 결과는 학습 시간 내림차순 — 유령이 1, 3번째를 차지해도 실제 공부 중인 3명이 순서대로 남는다
        val users = listOf(
            user("ghost1", now - 15 * 60 * 1000L, 500),
            user("a", now, 400),
            user("ghost2", null, 300),
            user("b", now, 200),
            user("c", now, 100),
            user("d", now, 50)
        )
        assertEquals(listOf("a", "b", "c"), uids(ObserveStudyingUsersUseCase.filterActive(users, now)))
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<StudyingUser>(), ObserveStudyingUsersUseCase.filterActive(emptyList(), now))
    }

    // --- 구독 (invoke) ---

    private fun TestScope.createUseCase(source: MutableStateFlow<List<StudyingUser>>) =
        ObserveStudyingUsersUseCase(FakeStudyingRepository(studyingUsers = source)) { now + testScheduler.currentTime }

    @Test
    fun `refilters periodically so ghost disappears without new snapshot`() = runTest {
        // 마지막 갱신이 기준 시간 1분 전 — 지금은 보이고, 리스너가 다시 불리지 않아도 1분 뒤 다시 걸러 사라져야 한다
        val source = MutableStateFlow(listOf(user("ghost", now - STALE_THRESHOLD_MS + REFILTER_INTERVAL_MS), user("a", now + 10 * 60 * 1000L)))
        val emitted = mutableListOf<List<StudyingUser>>()
        val job = launch { createUseCase(source)("tag").toList(emitted) }

        runCurrent()
        assertEquals(listOf("ghost", "a"), uids(emitted.last()))

        advanceTimeBy(REFILTER_INTERVAL_MS)
        runCurrent()
        assertEquals(listOf("a"), uids(emitted.last()))
        job.cancel()
    }

    @Test
    fun `new snapshot is filtered immediately`() = runTest {
        val source = MutableStateFlow(listOf(user("a", now)))
        val emitted = mutableListOf<List<StudyingUser>>()
        val job = launch { createUseCase(source)("tag").toList(emitted) }
        runCurrent()

        source.value = listOf(user("old", null), user("b", now))
        runCurrent()
        assertEquals(listOf("b"), uids(emitted.last()))
        job.cancel()
    }

    @Test
    fun `keeps refiltering last list after source completes`() = runTest {
        // Repository의 catch가 구독 에러 뒤 Flow를 끝내도, 마지막 목록의 유령은 주기마다 계속 사라져야 한다
        val source = flow { emit(listOf(user("ghost", now - STALE_THRESHOLD_MS + REFILTER_INTERVAL_MS))) }
        val useCase = ObserveStudyingUsersUseCase(FakeStudyingRepository(studyingUsers = source)) { now + testScheduler.currentTime }
        val emitted = mutableListOf<List<StudyingUser>>()
        val job = launch { useCase("tag").toList(emitted) }

        runCurrent()
        assertEquals(listOf("ghost"), uids(emitted.last()))
        advanceTimeBy(REFILTER_INTERVAL_MS)
        runCurrent()
        assertEquals(emptyList<String>(), uids(emitted.last()))
        job.cancel()
    }
}
