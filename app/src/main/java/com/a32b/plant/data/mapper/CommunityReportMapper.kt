package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.data.model.CommunityReportDto
import com.a32b.plant.domain.model.CommunityReport
import com.a32b.plant.domain.type.ReportReason

fun CommunityReportDto.toDomain(): CommunityReport = CommunityReport(
    id = id,
    targetType = targetType,
    targetId = targetId,
    reporterId = reporterId,
    reason = ReportReason.entries.firstOrNull { it.firestoreKey == reason } ?: ReportReason.OTHER,
    detail = detail,
    createdAt = createdAt.toLong(),
    status = status
)
