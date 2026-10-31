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
            // 정상 사용 중에는 대상이 삭제됐거나 같은 신고가 동시에 제출된 경우에만 발생
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                AppError.Custom("삭제되었거나 신고할 수 없는 콘텐츠입니다.")
            else -> AppError.Custom("신고 처리 중 오류가 발생했습니다.")
        }
        else -> AppError.Unknown()
    }
}
