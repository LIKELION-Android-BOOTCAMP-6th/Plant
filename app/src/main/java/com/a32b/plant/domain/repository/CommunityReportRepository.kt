package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ReportReason

interface CommunityReportRepository {
    suspend fun createReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: ReportReason,
        detail: String?
    ): Result<Unit>
}
