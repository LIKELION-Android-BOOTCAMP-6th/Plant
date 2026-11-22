package com.a32b.plant.data.source.remote.config

import com.a32b.plant.data.model.AppConfigDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AppConfigRemoteDataSource {

    override suspend fun getAppConfigFromServer(): AppConfigDto? =
        appConfigRef().get(Source.SERVER).await().toAppConfigDto()

    override suspend fun getAppConfigFromCache(): AppConfigDto? =
        appConfigRef().get(Source.CACHE).await().toAppConfigDto()

    private fun appConfigRef() = db.collection(CONFIG_COLLECTION).document(APP_DOCUMENT)

    // 타입을 확인하지 않고 필드 값을 그대로 담는다 (AppConfigDto 주석 참고)
    private fun DocumentSnapshot.toAppConfigDto(): AppConfigDto? {
        if (!exists()) return null
        return AppConfigDto(
            minVersionCode = get(FIELD_MIN_VERSION_CODE),
            message = get(FIELD_MESSAGE)
        )
    }

    private companion object {
        const val CONFIG_COLLECTION = "config"
        const val APP_DOCUMENT = "app"
        const val FIELD_MIN_VERSION_CODE = "minVersionCode"
        const val FIELD_MESSAGE = "message"
    }
}
