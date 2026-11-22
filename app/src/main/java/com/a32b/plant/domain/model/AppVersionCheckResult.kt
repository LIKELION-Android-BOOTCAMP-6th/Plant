package com.a32b.plant.domain.model

/** 앱 버전 확인 결과 (#662) */
sealed interface AppVersionCheckResult {
    /** 최소 지원 버전 이상이거나, 확인할 수 없어 통과 */
    data object UpToDate : AppVersionCheckResult

    /** 최소 지원 버전보다 낮아 업데이트해야 쓸 수 있음. message가 null이면 화면의 기본 문구 사용 */
    data class UpdateRequired(val message: String?) : AppVersionCheckResult
}
