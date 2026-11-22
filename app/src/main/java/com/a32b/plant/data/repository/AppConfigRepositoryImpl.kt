package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.hasInvalidMinVersionCode
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.model.AppConfigDto
import com.a32b.plant.data.source.remote.config.AppConfigRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.repository.AppConfigRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepositoryImpl @Inject constructor(
    private val remote: AppConfigRemoteDataSource
) : AppConfigRepository {

    // 스플래시가 이 응답을 기다리므로 서버는 짧게만 기다리고, 늦으면 캐시로 판단한다 (#662)
    override suspend fun getAppVersionPolicy(): Result<AppVersionPolicy?> =
        safeRunCatching {
            // 문서가 없는 경우(null)와 구분되도록 시간 초과만 ServerRead 없이(null) 받는다
            withTimeoutOrNull(SERVER_TIMEOUT_MS) { ServerRead(remote.getAppConfigFromServer()) }
        }.fold(
            onSuccess = { read ->
                if (read != null) Result.Success(read.dto?.toPolicy()) else getFromCache()
            },
            onFailure = { e ->
                if (isNetworkError(e)) getFromCache() else Result.Failure(handleError(e))
            }
        )

    private suspend fun getFromCache(): Result<AppVersionPolicy?> = safeRunCatching {
        remote.getAppConfigFromCache()?.toPolicy()
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e)) }
    )

    private fun AppConfigDto.toPolicy(): AppVersionPolicy {
        if (hasInvalidMinVersionCode()) {
            Log.w(TAG, "config/app의 minVersionCode가 정수가 아니라 버전 확인을 건너뜀: $minVersionCode")
        }
        return toDomain()
    }

    private fun isNetworkError(e: Throwable): Boolean = when (e) {
        is FirebaseFirestoreException ->
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        is FirebaseNetworkException -> true
        else -> false
    }

    private fun handleError(e: Throwable): AppError = when (e) {
        is AppError -> e
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
            else -> AppError.Custom("앱 설정 조회 실패")
        }
        is FirebaseNetworkException -> AppError.Network()
        else -> AppError.Unknown()
    }

    private class ServerRead(val dto: AppConfigDto?)

    private companion object {
        const val TAG = "AppConfigRepository"
        const val SERVER_TIMEOUT_MS = 2_000L
    }
}
