package com.a32b.plant.data.firestore

import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * [awaitServerWrite]를 Firestore 쓰기 Task 대신 [TaskCompletionSource]로 검증한다.
 * (awaitPendingWrites, pendingWritesFlow는 Firestore SDK를 그대로 감싼 것이라 #453에서 실제 흐름으로 확인)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PendingWritesTest {

    private val timeout = 5.seconds

    @Test
    fun `server ack within timeout returns Confirmed`() = runTest {
        val write = TaskCompletionSource<Void?>()
        val result = async { write.task.awaitServerWrite(timeout) }

        advanceTimeBy(1_000)
        write.setResult(null)

        assertEquals(WriteResult.Confirmed, result.await())
    }

    @Test
    fun `server rejection returns Rejected with original error`() = runTest {
        val write = TaskCompletionSource<Void?>()
        val error = IllegalStateException("PERMISSION_DENIED")
        write.setException(error)

        val result = write.task.awaitServerWrite(timeout)

        // 테스트 환경의 코루틴 디버그 모드가 예외를 복사본으로 바꿀 수 있어 같은 객체 대신 종류·메시지로 비교
        assertTrue(result is WriteResult.Rejected)
        val rejected = (result as WriteResult.Rejected).error
        assertTrue(rejected is IllegalStateException)
        assertEquals(error.message, rejected.message)
    }

    // 오프라인: 쓰기가 대기열에 남아 응답이 오지 않음 → 실패가 아니라 Pending
    @Test
    fun `no response within timeout returns Pending`() = runTest {
        val write = TaskCompletionSource<Void?>()

        val result = write.task.awaitServerWrite(timeout)

        assertEquals(WriteResult.Pending, result)
    }

    // Pending으로 기다림을 멈춰도 쓰기 자체는 살아 있어 나중에 완료될 수 있다 (재전송하면 중복)
    @Test
    fun `write can still complete after Pending`() = runTest {
        val write = TaskCompletionSource<Void?>()

        assertEquals(WriteResult.Pending, write.task.awaitServerWrite(timeout))
        assertFalse(write.task.isComplete)

        write.setResult(null)
        assertTrue(write.task.isSuccessful)
    }
}
