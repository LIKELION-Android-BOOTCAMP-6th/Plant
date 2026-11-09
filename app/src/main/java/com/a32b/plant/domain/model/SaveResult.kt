package com.a32b.plant.domain.model

/**
 * 오프라인에서도 기기에 바로 반영되는 쓰기의 결과 (#563, #455와 공용).
 *
 * 둘 다 성공이다. 실패(서버 규칙 거부 등)는 `Result.Failure`로 전달한다.
 */
sealed interface SaveResult {
    /** 서버에 저장됨 */
    data object Saved : SaveResult

    /**
     * 제한 시간 안에 서버 응답이 없음 (대부분 오프라인).
     * 기기에는 이미 반영됐고, 쓰기는 대기열에 남아 연결되면 전송된다. → 같은 쓰기를 다시 보내지 않는다.
     */
    data object Pending : SaveResult
}
