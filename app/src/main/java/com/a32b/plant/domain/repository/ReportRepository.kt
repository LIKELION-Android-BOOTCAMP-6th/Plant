package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.ReportPotLogs
import com.a32b.plant.domain.result.Result

interface ReportRepository {
    suspend fun getPeriodData(uid: String, startInclusive: Long, endExclusive: Long): Result<List<ReportPotLogs>>
}
