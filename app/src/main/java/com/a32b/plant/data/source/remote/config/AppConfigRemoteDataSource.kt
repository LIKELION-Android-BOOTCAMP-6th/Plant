package com.a32b.plant.data.source.remote.config

import com.a32b.plant.data.model.AppConfigDto

interface AppConfigRemoteDataSource {
    /** 서버에서 `config/app`을 읽는다. 문서가 없으면 null */
    suspend fun getAppConfigFromServer(): AppConfigDto?

    /**
     * 기기 캐시에서 `config/app`을 읽는다.
     * 캐시가 "문서 없음"을 알고 있으면 null, 캐시에 문서 정보가 없으면 FirebaseFirestoreException(UNAVAILABLE)
     */
    suspend fun getAppConfigFromCache(): AppConfigDto?
}
