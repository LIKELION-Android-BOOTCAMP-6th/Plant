package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.result.Result

interface AppConfigRepository {
    /**
     * 최소 지원 버전 설정을 읽는다 (#662).
     * 서버 응답이 늦거나 오프라인이면 기기 캐시 값을 돌려준다.
     * 문서가 없으면 Success(null), 서버·캐시 모두 읽지 못하면 Failure
     */
    suspend fun getAppVersionPolicy(): Result<AppVersionPolicy?>
}
