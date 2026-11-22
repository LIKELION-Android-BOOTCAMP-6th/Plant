package com.a32b.plant.fake

import com.a32b.plant.domain.model.AppVersionPolicy
import com.a32b.plant.domain.repository.AppConfigRepository
import com.a32b.plant.domain.result.Result

/** 테스트 공용 [AppConfigRepository]. getAppVersionPolicy는 [result]를 그대로 돌려준다 */
class FakeAppConfigRepository(
    var result: Result<AppVersionPolicy?> = Result.Success(null)
) : AppConfigRepository {
    var calls = 0

    override suspend fun getAppVersionPolicy(): Result<AppVersionPolicy?> {
        calls++
        return result
    }
}
