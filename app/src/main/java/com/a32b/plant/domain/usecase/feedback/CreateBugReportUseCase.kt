package com.a32b.plant.domain.usecase.feedback

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CreateBugReportUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(
        feedbackId: String,
        category: BugCategory,
        description: String,
        reproductionSteps: String?,
        deviceModel: String,
        androidVersion: String,
        appVersion: String
    ): Result<FeedbackSubmissionResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val feedback = Feedback.createBug(
            reporterId = user.uid,
            category = category,
            description = description,
            reproductionSteps = reproductionSteps?.takeIf { it.isNotBlank() },
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            appVersion = appVersion
        )

        return feedbackRepository.createBugReport(
            feedbackId = feedbackId,
            feedback = feedback
        )
    }
}