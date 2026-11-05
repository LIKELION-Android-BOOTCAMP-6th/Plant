package com.a32b.plant.data.firestore

import com.a32b.plant.core.util.safeRunCatching
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

/*
 * 오프라인 쓰기의 서버 반영 확인 공통 함수 (#452).
 *
 * Firestore 쓰기(set/update/delete/batch)는 오프라인이어도 기기 캐시에 바로 반영되고,
 * 서버 전송 대기열에 남아 있다가 연결되면 보낸 순서대로 전송된다. (앱을 재시작해도 대기열은 유지됨)
 * 서버에 도착하면 보안 규칙이 검사하고, 거부되면 기기 캐시도 되돌아간다.
 *
 * 따라서 "제한 시간 안에 응답이 없다"는 실패가 아니라 **아직 대기 중**이다.
 * 이때 같은 쓰기를 다시 보내면 대기열의 원래 쓰기와 합쳐 중복 저장된다 (#505).
 */

/** 쓰기 하나의 서버 반영 결과 */
sealed interface WriteResult {
    /** 서버가 받아들임 → 로컬 백업을 지워도 된다 */
    data object Confirmed : WriteResult

    /** 서버(보안 규칙 등)가 거부함 → 기기 캐시도 되돌아갔다. [error]는 호출부에서 AppError로 매핑 */
    data class Rejected(val error: Throwable) : WriteResult

    /**
     * 제한 시간 안에 서버 응답이 없음 (대부분 오프라인).
     * **실패가 아니다.** 쓰기는 대기열에 남아 연결되면 전송된다.
     * → 로컬 백업은 유지하되 같은 쓰기를 다시 보내지 않는다.
     */
    data object Pending : WriteResult
}

/**
 * 이미 시작한 쓰기 [Task]의 서버 반영을 [timeout]만큼만 기다린다.
 *
 * 사용 예:
 * ```
 * val write = batch.commit()   // 쓰기는 이 시점에 시작됨 (대기열 등록)
 * when (write.awaitServerWrite(SAVE_TIMEOUT)) {
 *     WriteResult.Confirmed -> 백업 삭제
 *     is WriteResult.Rejected -> 안내 + 백업 정리
 *     WriteResult.Pending -> "연결되면 자동으로 반영돼요" 안내, 백업 유지 (재전송 금지)
 * }
 * ```
 * 제한 시간이 지나 기다림을 멈춰도 쓰기 자체는 취소되지 않는다 (Firestore 쓰기는 취소할 수 없음).
 */
suspend fun Task<*>.awaitServerWrite(timeout: Duration): WriteResult =
    safeRunCatching {
        withTimeoutOrNull(timeout) {
            await()
            WriteResult.Confirmed
        } ?: WriteResult.Pending
    }.getOrElse { WriteResult.Rejected(it) }

/**
 * 지금까지 보낸 모든 쓰기(앱 재시작 전에 보낸 것 포함)가 서버에서 처리될 때까지 [timeout]만큼 기다린다.
 * - true: 대기열이 비었다 (모두 반영 또는 거부됨) → 이제 서버 상태를 조회해 판단해도 된다
 * - false: **아직 판단하지 말 것.** 다음 경우 모두 false
 *   - 시간 안에 비지 않음 (오프라인)
 *   - 기다리는 중 사용자가 바뀜 (로그아웃·계정 전환 시 SDK가 CANCELLED 예외로 끝냄)
 *   - 그 외 예외
 *
 * 처리 결과(성공/거부)는 알려주지 않는다. 필요하면 대기열이 빈 뒤 서버에서 문서를 조회해 확인한다.
 * 예: 홈 복구 창을 띄우기 전에 호출 → 대기열이 비었는데 서버에 기록이 있으면 이미 저장된 것이므로 백업만 삭제.
 *
 * 이 함수를 호출한 **뒤에** 보낸 쓰기는 기다리지 않는다.
 */
suspend fun FirebaseFirestore.awaitPendingWrites(timeout: Duration): Boolean =
    safeRunCatching {
        withTimeoutOrNull(timeout) {
            waitForPendingWrites().await()
            true
        } ?: false
    }.getOrElse { false }

/**
 * 이 문서에 서버 반영 대기 중인 쓰기가 있는지 실시간으로 알려준다 (true = 대기 중).
 * 화면에 "동기화 대기 중" 같은 표시를 할 때 사용.
 *
 * 주의
 * - false가 됐다고 성공한 것은 아니다. 규칙에서 거부돼 되돌아가도 false가 된다.
 *   성공 여부는 [awaitServerWrite] 결과나 문서 내용으로 판단한다.
 * - 리스너 에러(예: 로그아웃 후 PERMISSION_DENIED)는 Flow 예외로 전달된다 → 호출부에서 `.catch` 필수.
 *   에러를 false로 바꾸면 "대기 없음"과 구분되지 않으므로 여기서 삼키지 않는다.
 */
fun DocumentReference.pendingWritesFlow(): Flow<Boolean> =
    snapshots(MetadataChanges.INCLUDE)
        .map { it.metadata.hasPendingWrites() }
        .distinctUntilChanged()
