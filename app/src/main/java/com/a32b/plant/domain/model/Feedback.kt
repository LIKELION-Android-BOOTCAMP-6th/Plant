package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.type.FeedbackCategory
import com.a32b.plant.domain.type.FeedbackType
import com.a32b.plant.domain.type.ImprovementCategory

data class Feedback(
    val type: FeedbackType,
    val reporterId: String,
    val category: FeedbackCategory,
    val description: String,
    val appVersion: String,
    val reproductionSteps: String?,
    val deviceModel: String?,
    val androidVersion: String?
) {
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
            type = FeedbackType.BUG,
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
            type = FeedbackType.IMPROVEMENT,
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
