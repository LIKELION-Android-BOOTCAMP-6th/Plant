package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.ReportRecord
import com.a32b.plant.domain.repository.ReportRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

// 한국 날짜 [startDate 0시, endDate 0시) 사이에 시작한 기록을 화분 정보와 함께 가져온다.
class GetReportRecordsUseCase @Inject constructor(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(uid: String, startDate: LocalDate, endDate: LocalDate): Result<List<ReportRecord>> =
        reportRepository.getPeriodData(
            uid = uid,
            startInclusive = startDate.atStartOfDay(REPORT_ZONE).toInstant().toEpochMilli(),
            endExclusive = endDate.atStartOfDay(REPORT_ZONE).toInstant().toEpochMilli()
        ).map { periodData ->
            periodData.potLogs.flatMap { potLogs ->
                // 현재 조회 경로(StudyLogMapper)에서는 createAt이 항상 값이 있어 제외되는 기록이 없다.
                // StudyLog.createAt 타입이 Long?이라 null 처리만 둔다.
                potLogs.logs.mapNotNull { log ->
                    val endMillis = log.createAt ?: return@mapNotNull null
                    ReportRecord(
                        potName = potLogs.pot.name,
                        tagName = potLogs.pot.tagName,
                        studyingTime = log.studyingTime,
                        startedAt = Instant.ofEpochMilli(log.startedAt).atZone(REPORT_ZONE).toLocalDateTime(),
                        endedAt = Instant.ofEpochMilli(endMillis).atZone(REPORT_ZONE).toLocalDateTime()
                    )
                }
            }
        }
}

private val REPORT_ZONE = ZoneId.of("Asia/Seoul")
