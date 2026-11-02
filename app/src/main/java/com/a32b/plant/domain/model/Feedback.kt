package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.type.FeedbackCategory
import com.a32b.plant.domain.type.FeedbackType
import com.a32b.plant.domain.type.ImprovementCategory

data class Feedback(
    val reporterId: String,
    val category: FeedbackCategory,
    val description: String,
    val appVersion: String,
    val reproductionSteps: String?,
    val deviceModel: String?,
    val androidVersion: String?
) {
    val type: FeedbackType
        get() = when (category) {
            is BugCategory -> FeedbackType.BUG
            is ImprovementCategory -> FeedbackType.IMPROVEMENT
        }

    companion object {
        fun createBug(
            reporterId: String,
            category: BugCategory,
            description: String,
            reproductionSteps: String?,
            deviceModel: String,
            androidVersion: String,
            appVersion: String
        ) = Feedback(
            reporterId = reporterId,
            category = category,
            description = description,
            appVersion = appVersion,
            reproductionSteps = reproductionSteps,
            deviceModel = deviceModel,
            androidVersion = androidVersion
        )

        fun createImprovement(
            reporterId: String,
            category: ImprovementCategory,
            description: String,
            appVersion: String
        ) = Feedback(
            reporterId = reporterId,
            category = category,
            description = description,
            appVersion = appVersion,
            reproductionSteps = null,
            deviceModel = null,
            androidVersion = null
        )
    }
}
