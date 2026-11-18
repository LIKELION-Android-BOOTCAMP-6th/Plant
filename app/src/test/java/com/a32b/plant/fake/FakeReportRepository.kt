package com.a32b.plant.fake

import com.a32b.plant.domain.model.ReportPeriodData
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.delay

/**
 * 테스트 공용 [ReportRepository].
 * [periodData]가 있으면 호출 순서(0부터)별 결과를, 없으면 [isFromCache] 값의 빈 결과를 돌려준다.
 */
class FakeReportRepository(
    var isFromCache: Boolean = false,
    private val delayMillis: Long = 0L,
    private val periodData: ((Int) -> ReportPeriodData)? = null
) : ReportRepository {
    var calls = 0

    override suspend fun getPeriodData(
        uid: String,
        startInclusive: Long,
        endExclusive: Long
    ): Result<ReportPeriodData> {
        delay(delayMillis)
        val result = periodData?.invoke(calls) ?: ReportPeriodData(emptyList(), isFromCache)
        calls++
        return Result.Success(result)
    }
}
