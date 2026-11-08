package com.a32b.plant.domain.model

sealed interface FeedbackSubmissionResult {
    data object Saved : FeedbackSubmissionResult
    data object Pending : FeedbackSubmissionResult
}
