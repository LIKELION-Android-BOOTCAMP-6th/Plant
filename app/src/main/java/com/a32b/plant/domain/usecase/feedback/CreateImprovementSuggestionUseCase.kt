package com.a32b.plant.domain.usecase.feedback

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class CreateImprovementSuggestionUseCase @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(
        feedbackId: String,
        category: ImprovementCategory,
        description: String,
        appVersion: String
    ): Result<FeedbackSubmissionResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val feedback = Feedback.createImprovement(
            reporterId = user.uid,
            category = category,
            description = description,
            appVersion = appVersion
        )

        return feedbackRepository.createFeedback(
            feedbackId = feedbackId,
            feedback = feedback
        )
    }
}
