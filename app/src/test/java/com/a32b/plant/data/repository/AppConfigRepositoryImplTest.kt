package com.a32b.plant.data.repository

import com.a32b.plant.data.model.AppConfigDto
import com.a32b.plant.data.source.remote.config.AppConfigRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// JVM 유닛 테스트에서 확인할 수 없는 경로
// - FirebaseFirestoreException(UNAVAILABLE·PERMISSION_DENIED 등): Code enum이 초기화될 때 android.util.SparseArray를 써서
//   "not mocked" 예외가 난다. 실제 오프라인 서버 읽기는 이 예외(UNAVAILABLE)로 실패하므로, 그 코드 판별은 이 테스트가 확인하지 않는다.
//   여기서는 isNetworkError가 true를 내는 FirebaseNetworkException으로 "네트워크 오류면 캐시로 넘어간다"는 흐름만 확인한다
// - minVersionCode가 잘못된 값: android.util.Log를 호출한다. 값 해석은 AppConfigMapperTest에서 확인한다
@OptIn(ExperimentalCoroutinesApi::class)
class AppConfigRepositoryImplTest {
    private val serverDto = AppConfigDto(minVersionCode = 5L, message = "서버")
    private val cacheDto = AppConfigDto(minVersionCode = 4L, message = "캐시")

    @Test
    fun serverValueIsUsedWithoutReadingCache() = runTest {
        val remote = FakeAppConfigRemoteDataSource(server = { serverDto }, cache = { cacheDto })

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertEquals(Result.Success(AppVersionPolicy(5L, "서버")), result)
        assertEquals(0, remote.cacheCalls)
    }

    // 서버가 "문서 없음"이라고 답하면 캐시를 보지 않고 통과(null)
    @Test
    fun missingDocumentOnServerIsNullWithoutReadingCache() = runTest {
        val remote = FakeAppConfigRemoteDataSource(server = { null }, cache = { cacheDto })

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertEquals(Result.Success(null), result)
        assertEquals(0, remote.cacheCalls)
    }

    // 서버 읽기가 네트워크 오류로 실패 → 캐시 값으로 판단
    @Test
    fun offlineUsesCachedValue() = runTest {
        val remote = FakeAppConfigRemoteDataSource(server = { throw networkException() }, cache = { cacheDto })

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertEquals(Result.Success(AppVersionPolicy(4L, "캐시")), result)
        assertEquals(1, remote.cacheCalls)
    }

    // 응답 지연: 2초에서 서버 읽기를 멈추고 캐시로 판단
    @Test
    fun slowServerFallsBackToCacheAfterTwoSeconds() = runTest {
        val remote = FakeAppConfigRemoteDataSource(server = { awaitCancellation() }, cache = { cacheDto })

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertEquals(Result.Success(AppVersionPolicy(4L, "캐시")), result)
        assertEquals(2_000L, currentTime)
    }

    @Test
    fun serverJustUnderTimeoutIsUsed() = runTest {
        val remote = FakeAppConfigRemoteDataSource(
            server = { delay(1_999L); serverDto },
            cache = { cacheDto }
        )

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertEquals(Result.Success(AppVersionPolicy(5L, "서버")), result)
        assertEquals(0, remote.cacheCalls)
    }

    // 응답 지연 + 캐시에도 없음 → Failure (UseCase가 통과 처리)
    // 실제 캐시 미스는 FirebaseFirestoreException(UNAVAILABLE)이지만 위 주석의 이유로 다른 예외로 대신한다
    @Test
    fun slowServerWithoutCacheIsFailure() = runTest {
        val remote = FakeAppConfigRemoteDataSource(
            server = { awaitCancellation() },
            cache = { throw IllegalStateException("캐시에 문서 정보 없음") }
        )

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertTrue(result is Result.Failure)
        assertEquals(2_000L, currentTime)
    }

    // 서버·캐시 모두 네트워크 오류 → Failure(Network)
    @Test
    fun offlineWithoutCacheIsNetworkFailure() = runTest {
        val remote = FakeAppConfigRemoteDataSource(
            server = { throw networkException() },
            cache = { throw networkException() }
        )

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertTrue(result is Result.Failure && result.error is AppError.Network)
    }

    // 캐시가 "문서 없음"을 알고 있으면 null
    @Test
    fun cacheKnowsMissingDocumentIsNull() = runTest {
        val remote = FakeAppConfigRemoteDataSource(server = { throw networkException() }, cache = { null })

        assertEquals(Result.Success(null), AppConfigRepositoryImpl(remote).getAppVersionPolicy())
    }

    // 네트워크 오류가 아닌 실패(권한 거부 등)는 다시 읽어도 같으므로 캐시를 보지 않고 Failure (UseCase가 통과 처리)
    @Test
    fun nonNetworkFailureIsFailureWithoutReadingCache() = runTest {
        val remote = FakeAppConfigRemoteDataSource(
            server = { throw IllegalStateException("권한 거부 등") },
            cache = { cacheDto }
        )

        val result = AppConfigRepositoryImpl(remote).getAppVersionPolicy()

        assertTrue(result is Result.Failure && result.error is AppError.Unknown)
        assertEquals(0, remote.cacheCalls)
    }

    // FirebaseNetworkException 생성자는 부모(FirebaseException)에서 android.text.TextUtils.isEmpty를 불러
    // JVM 유닛 테스트에서는 "not mocked" 예외가 난다. 생성자를 거치지 않고 객체만 만든다
    private fun networkException(): FirebaseNetworkException {
        val unsafe = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe")
            .apply { isAccessible = true }
            .get(null)
        return unsafe.javaClass.getMethod("allocateInstance", Class::class.java)
            .invoke(unsafe, FirebaseNetworkException::class.java) as FirebaseNetworkException
    }

    private class FakeAppConfigRemoteDataSource(
        private val server: suspend () -> AppConfigDto?,
        private val cache: suspend () -> AppConfigDto?
    ) : AppConfigRemoteDataSource {
        var cacheCalls = 0

        override suspend fun getAppConfigFromServer(): AppConfigDto? = server()

        override suspend fun getAppConfigFromCache(): AppConfigDto? {
            cacheCalls++
            return cache()
        }
    }
}
