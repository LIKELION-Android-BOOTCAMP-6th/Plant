package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.datasource.StudyLogRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.ReportMonthData
import com.a32b.plant.domain.model.ReportPotLogs
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val potRemoteDataSource: PotRemoteDataSource,
    private val studyLogRemoteDataSource: StudyLogRemoteDataSource
) : ReportRepository {

    //화분 목록 1회 조회 + 화분별 기간 조회(동시 실행)를 한데 묶어 반환한다.
    //화분 하나라도 실패하면 부분 결과 없이 전체 실패로 처리한다(awaitAll이 첫 예외에서 나머지도 취소하고 그대로 던짐).
    override suspend fun getMonthData(uid: String, month: YearMonth): Result<ReportMonthData> =
        safeRunCatching {
            val (startInclusive, endExclusive) = monthRangeMillis(month)

            val pots = potRemoteDataSource.getPotsOnce(uid).map { it.toDomain() }

            val potLogs = coroutineScope {
                pots.map { pot ->
                    async {
                        val logs = studyLogRemoteDataSource
                            .getPotLogsInPeriod(uid, pot.id, startInclusive.toTimestamp(), endExclusive.toTimestamp())
                            .map { it.toDomain() }
                        ReportPotLogs(pot = pot, logs = logs)
                    }
                }.awaitAll()
            }

            ReportMonthData(potLogs = potLogs)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )

    //KST 기준 [선택월 1일-1일 00:00, 다음달 1일+2일 00:00) — 2026-09-26 확정, 밀리초로 반환.
    private fun monthRangeMillis(month: YearMonth): Pair<Long, Long> {
        val zone = ZoneId.of("Asia/Seoul")
        val startInclusive = month.atDay(1).minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = month.plusMonths(1).atDay(1).plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
        return startInclusive to endExclusive
    }

    private fun mapToAppError(throwable: Throwable): AppError {
        if (throwable is AppError) return throwable
        Log.e("ReportRepositoryImpl", "Original error: ${throwable.stackTraceToString()}")
        return when (throwable) {
            is FirebaseNetworkException -> AppError.Network()
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
                    FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network()
                    else -> AppError.Server()
                }
            }
            else -> AppError.Unknown()
        }
    }
}
