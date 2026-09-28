package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.ReportMonthData
import com.a32b.plant.domain.result.Result
import java.time.YearMonth

interface ReportRepository {
    suspend fun getMonthData(uid: String, month: YearMonth): Result<ReportMonthData>
}
