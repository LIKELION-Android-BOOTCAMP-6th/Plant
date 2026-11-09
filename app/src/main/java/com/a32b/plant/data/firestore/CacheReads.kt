package com.a32b.plant.data.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * 기기 캐시에서 문서를 읽는다. 캐시에 없을 때만 기본 조회(온라인이면 서버)로 읽는다 (#455).
 *
 * 쓰기 값을 저장된 문서에서 정할 때 사용한다 (예: 기록 삭제 시 차감할 학습 시간).
 * - 캐시에는 아직 서버에 보내지 않은 쓰기(대기열)도 반영되어 있다.
 *   대기열은 보낸 순서대로 서버에 도착하므로, 이 값으로 만든 쓰기는 서버에서도 같은 값으로 검증된다.
 * - 오프라인에서도 바로 읽힌다.
 * - 다른 기기에서 바뀐 값이 아직 캐시에 없으면 규칙이 거부한다 → 호출부가 서버 상태를 조회해 판단한다.
 *
 * 캐시가 "문서 없음"을 알고 있으면 exists() == false인 스냅샷을 돌려준다.
 */
suspend fun DocumentReference.getPreferringCache(): DocumentSnapshot =
    try {
        get(Source.CACHE).await()
    } catch (e: FirebaseFirestoreException) {
        // 캐시에 문서 정보가 없으면 UNAVAILABLE
        if (e.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw e
        get().await()
    }

/**
 * 다시 보내도 결과가 같은 서버 거부인지 — 보안 규칙 거부(PERMISSION_DENIED), 없는 문서 update(NOT_FOUND).
 * 이런 거부는 서버 상태를 조회해 "이미 처리됨"(연타·재전송으로 먼저 도착한 쓰기)인지 확인한다.
 */
fun Throwable.isRuleRejection(): Boolean =
    this is FirebaseFirestoreException && (
        code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            code == FirebaseFirestoreException.Code.NOT_FOUND
        )
