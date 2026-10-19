package com.a32b.plant.domain.usecase.report

import com.a32b.plant.core.util.KST_ZONE_ID
import com.a32b.plant.domain.model.ReportRecord
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

// 한국 날짜 [startDate 0시, endDate 0시) 사이에 시작한 기록을 화분 정보와 함께 가져온다.
class GetReportRecordsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(uid: String, startDate: LocalDate, endDate: LocalDate): Result<List<ReportRecord>> =
        reportRepository.getPeriodData(
            uid = uid,
            startInclusive = startDate.atStartOfDay(KST_ZONE_ID).toInstant().toEpochMilli(),
            endExclusive = endDate.atStartOfDay(KST_ZONE_ID).toInstant().toEpochMilli()
        ).map { potLogsList ->
            potLogsList.flatMap { potLogs ->
                // 현재 조회 경로(StudyLogMapper)에서는 createAt이 항상 값이 있어 제외되는 기록이 없다.
                // StudyLog.createAt 타입이 Long?이라 null 처리만 둔다.
                potLogs.logs.mapNotNull { log ->
                    val endMillis = log.createAt ?: return@mapNotNull null
                    ReportRecord(
                        potName = potLogs.pot.name,
                        tagName = potLogs.pot.tagName,
                        studyingTime = log.studyingTime,
                        startedAt = Instant.ofEpochMilli(log.startedAt).atZone(KST_ZONE_ID).toLocalDateTime(),
                        endedAt = Instant.ofEpochMilli(endMillis).atZone(KST_ZONE_ID).toLocalDateTime()
                    )
                }
            }
        }
}
