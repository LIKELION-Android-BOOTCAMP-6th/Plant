package com.a32b.plant.domain.type

enum class ImprovementCategory(
    override val label: String,
    override val firestoreValue: String
) : FeedbackCategory {
    UI_IMPROVEMENT("UI 개선", "ui_improvement"),
    NEW_FEATURE("새 기능", "new_feature"),
    CONVENIENCE("편의성", "convenience"),
    OTHER("기타", "other")
}
