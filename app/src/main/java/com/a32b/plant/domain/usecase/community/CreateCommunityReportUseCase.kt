package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.repository.CommunityReportRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ReportReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CreateCommunityReportUseCase @Inject constructor(
    private val repository: CommunityReportRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: ReportReason,
        detail: String?
    ): Result<Unit> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.createReport(
            reporterUid = user.uid,
            targetType = targetType,
            targetId = targetId,
            parentPostId = parentPostId,
            reason = reason,
            detail = detail?.takeIf { it.isNotBlank() }
        )
    }
}
