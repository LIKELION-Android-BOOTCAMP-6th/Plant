package com.a32b.plant.core.util

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Firestore 쓰기 작업(set/update/runBatch)은 오프라인이어도 로컬 큐에 즉시 등록되고,
 * 재연결 시 그대로 서버에 반영된다. 즉 [block]을 타임아웃으로 "포기"해버리면 이미 큐잉된 쓰기
 * 자체는 취소되지 않는데도 "실패했다"는 잘못된 신호를 줘서 사용자가 재시도하게 만들고,
 * 그 결과 중복 쓰기가 쌓인다.
 *
 * 그래서 [block]은 끝까지 실행되도록 두고, [threshold]가 지나도 끝나지 않으면 [onSlow]로
 * "느리다"는 안내만 한 번 보여준다. 실제 성공/실패 결과는 [block]이 실제로 끝났을 때만 반환된다.
 */
suspend fun <T> withSlowNotice(
    threshold: Duration = NETWORK_SLOW_THRESHOLD,
    onSlow: () -> Unit,
    block: suspend () -> T
): T = coroutineScope {
    val noticeJob = launch {
        delay(threshold)
        onSlow()
    }
    try {
        block()
    } finally {
        noticeJob.cancel()
    }
}

val NETWORK_SLOW_THRESHOLD = 2.seconds
const val NETWORK_SLOW_MESSAGE = "네트워크가 불안정합니다. 연결되면 자동으로 처리됩니다."
