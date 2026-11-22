package com.a32b.plant.domain.usecase.appversion

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AppVersionCheckResult
import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.result.Result
import com.a32b.plant.fake.FakeAppConfigRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckAppVersionUseCaseTest {
    private fun check(result: Result<AppVersionPolicy?>, currentVersionCode: Long): AppVersionCheckResult =
        runBlocking { CheckAppVersionUseCase(FakeAppConfigRepository(result))(currentVersionCode) }

    private fun policy(minVersionCode: Long?, message: String? = null) =
        Result.Success(AppVersionPolicy(minVersionCode, message))

    @Test
    fun currentVersionHigherThanMinimumIsUpToDate() {
        assertEquals(AppVersionCheckResult.UpToDate, check(policy(3), currentVersionCode = 4))
    }

    @Test
    fun currentVersionEqualToMinimumIsUpToDate() {
        assertEquals(AppVersionCheckResult.UpToDate, check(policy(3), currentVersionCode = 3))
    }

    @Test
    fun currentVersionLowerThanMinimumRequiresUpdateWithMessage() {
        assertEquals(
            AppVersionCheckResult.UpdateRequired("새 버전이 나왔어요"),
            check(policy(3, "새 버전이 나왔어요"), currentVersionCode = 2)
        )
    }

    @Test
    fun updateRequiredWithoutMessagePassesNull() {
        assertEquals(AppVersionCheckResult.UpdateRequired(null), check(policy(3), currentVersionCode = 2))
    }

    // config/app 문서가 없음
    @Test
    fun missingDocumentIsUpToDate() {
        assertEquals(AppVersionCheckResult.UpToDate, check(Result.Success(null), currentVersionCode = 1))
    }

    // minVersionCode가 없거나 잘못된 값(매퍼가 null로 바꿈)
    @Test
    fun missingOrInvalidMinimumIsUpToDate() {
        assertEquals(AppVersionCheckResult.UpToDate, check(policy(null, "안내"), currentVersionCode = 1))
    }

    // 서버·캐시 모두 읽지 못함(오프라인 + 캐시 없음), 권한 거부 등
    @Test
    fun readFailureIsUpToDate() {
        assertEquals(AppVersionCheckResult.UpToDate, check(Result.Failure(AppError.Network()), currentVersionCode = 1))
        assertEquals(AppVersionCheckResult.UpToDate, check(Result.Failure(AppError.Permission()), currentVersionCode = 1))
        assertEquals(AppVersionCheckResult.UpToDate, check(Result.Failure(AppError.Unknown()), currentVersionCode = 1))
    }
}
