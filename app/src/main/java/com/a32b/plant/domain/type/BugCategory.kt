package com.a32b.plant.domain.type

enum class BugCategory(
    override val label: String,
    override val firestoreKey: String
) : FeedbackCategory {
    UI_BUG("UI 버그", "ui_bug"),
    FEATURE_ERROR("기능 오류", "feature_error"),
    OTHER("기타", "other")
}
