package com.a32b.plant.data.source.remote.community

interface CommunityReportRemoteDataSource {
    suspend fun createReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: String,
        detail: String?
    ): Boolean
}
