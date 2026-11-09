package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.StudySaveRejection

/** 학습 기록 저장(기록 + 화분·유저 학습 시간 batch) 결과 (#453) */
sealed interface StudySaveResult {
    /** 서버에 저장됨 → 로컬 백업 삭제 */
    data object Saved : StudySaveResult

    /** 같은 세션이 이미 서버에 저장되어 있음 (대기열의 저장 + 복구 재전송 등) → 저장된 것으로 보고 로컬 백업 삭제 */
    data object AlreadySaved : StudySaveResult

    /**
     * 제한 시간 안에 서버 응답이 없음 (대부분 오프라인). **실패가 아니다.**
     * 기기 캐시에는 이미 반영됐고 연결되면 전송된다 → 로컬 백업은 유지하되 같은 저장을 다시 보내지 않는다
     */
    data object Pending : StudySaveResult

    /** 다시 보내도 해결되지 않는 거부 (검증 실패, 없는/완료된 화분 등) → 사유 안내 후 로컬 백업 삭제 */
    data class Rejected(val reason: StudySaveRejection) : StudySaveResult
}
