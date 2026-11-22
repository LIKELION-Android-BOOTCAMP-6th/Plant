package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.result.Result

interface AppConfigRepository {
    /**
     * 최소 지원 버전 설정을 읽는다 (#662).
     * 서버 응답이 늦거나(시간 초과) 네트워크 오류면 기기 캐시 값을 돌려준다.
     * 문서가 없으면 Success(null).
     * Failure: 서버가 네트워크 외 오류(권한 거부 등)로 실패하면 캐시를 보지 않고 바로, 캐시로 넘어갔는데 캐시에서도 읽지 못하면
     */
    suspend fun getAppVersionPolicy(): Result<AppVersionPolicy?>
}
