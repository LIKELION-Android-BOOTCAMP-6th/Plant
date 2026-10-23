package com.a32b.plant.data.repository

import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.remote.community.CommunityReportRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.CommunityReportRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ReportReason
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityReportRepositoryImpl @Inject constructor(
    private val remote: CommunityReportRemoteDataSource
) : CommunityReportRepository {

    override suspend fun createReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: ReportReason,
        detail: String?
    ): Result<Unit> = safeRunCatching {
        remote.createReport(
            reporterUid = reporterUid,
            targetType = targetType,
            targetId = targetId,
            parentPostId = parentPostId,
            reason = reason.firestoreKey,
            detail = detail
        )
    }.fold(
        onSuccess = { reported ->
            if (reported) Result.Success(Unit)
            else Result.Failure(AppError.Custom("이미 신고한 콘텐츠입니다."))
        },
        onFailure = { e -> Result.Failure(handleError(e)) }
    )

    private fun handleError(e: Throwable): AppError = when (e) {
        is AppError -> e
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
            else -> AppError.Custom("신고 처리 중 오류가 발생했습니다.")
        }
        else -> AppError.Unknown()
    }
}
