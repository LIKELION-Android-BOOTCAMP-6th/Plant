package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class GetTodayTotalStudyTimeUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    suspend operator fun invoke(uid: String, potId: String): Result<Long> {
        val startOfToday = LocalDate.now(ZoneId.of("Asia/Seoul"))
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .let { Date.from(it) }

        return when (val result = potRepository.getTodayStudyLogs(uid, potId, startOfToday)) {
            is Result.Success -> {
                val totalTime = result.data.sumOf { it.studyingTime }
                Result.Success(totalTime)
            }
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}