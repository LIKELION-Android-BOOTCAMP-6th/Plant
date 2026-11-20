package com.a32b.plant.data.community

import com.a32b.plant.domain.community.PostUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 게시글 생성(#669)·삭제(#712) 신호가 목록 화면 구독자에게 각자의 흐름으로만 전달되는지 확인한다.
 * 전송 실패 경로는 android.util.Log를 남겨 JVM 유닛 테스트에서 실행할 수 없으므로 확인하지 않는다
 * (DROP_OLDEST + 여유 버퍼라 tryEmit이 실패하지 않는다).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PostUpdateEventTest {

    @Test
    fun `생성 신호는 postCreated로만 전달되고 event로는 가지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        val created = mutableListOf<String>()
        val updates = mutableListOf<PostUpdate>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val createdJob = launch(dispatcher) { bus.postCreated.collect { created += it } }
        val updatesJob = launch(dispatcher) { bus.event.collect { updates += it } }

        bus.notifyPostCreated("post-1")

        assertEquals(listOf("post-1"), created)
        assertTrue(updates.isEmpty())
        createdJob.cancel()
        updatesJob.cancel()
    }

    @Test
    fun `수정 신호는 event로만 전달되고 postCreated로는 가지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        val created = mutableListOf<String>()
        val updates = mutableListOf<PostUpdate>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val createdJob = launch(dispatcher) { bus.postCreated.collect { created += it } }
        val updatesJob = launch(dispatcher) { bus.event.collect { updates += it } }

        val update = PostUpdate(postId = "post-1", title = "수정된 제목")
        bus.notifyPostUpdated(update)

        assertEquals(listOf(update), updates)
        assertTrue(created.isEmpty())
        createdJob.cancel()
        updatesJob.cancel()
    }

    @Test
    fun `삭제 신호는 postDeleted로만 전달되고 event와 postCreated로는 가지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        val deleted = mutableListOf<String>()
        val created = mutableListOf<String>()
        val updates = mutableListOf<PostUpdate>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val deletedJob = launch(dispatcher) { bus.postDeleted.collect { deleted += it } }
        val createdJob = launch(dispatcher) { bus.postCreated.collect { created += it } }
        val updatesJob = launch(dispatcher) { bus.event.collect { updates += it } }

        bus.notifyPostDeleted("post-1")

        assertEquals(listOf("post-1"), deleted)
        assertTrue(created.isEmpty())
        assertTrue(updates.isEmpty())
        deletedJob.cancel()
        createdJob.cancel()
        updatesJob.cancel()
    }

    @Test
    fun `생성·수정 신호는 postDeleted로 가지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        val deleted = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { bus.postDeleted.collect { deleted += it } }

        bus.notifyPostCreated("post-1")
        bus.notifyPostUpdated(PostUpdate(postId = "post-2", title = "수정된 제목"))

        assertTrue(deleted.isEmpty())
        job.cancel()
    }

    @Test
    fun `구독 전에 보낸 삭제 신호는 다시 받지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        bus.notifyPostDeleted("before-subscribe")

        val deleted = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { bus.postDeleted.collect { deleted += it } }
        bus.notifyPostDeleted("after-subscribe")

        assertEquals(listOf("after-subscribe"), deleted)
        job.cancel()
    }

    @Test
    fun `구독 전에 보낸 생성 신호는 다시 받지 않는다`() = runTest {
        val bus = PostUpdateEvent()
        bus.notifyPostCreated("before-subscribe")

        val created = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { bus.postCreated.collect { created += it } }
        bus.notifyPostCreated("after-subscribe")

        assertEquals(listOf("after-subscribe"), created)
        job.cancel()
    }
}
