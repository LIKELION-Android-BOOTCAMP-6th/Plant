package com.a32b.plant.domain.type

sealed interface FeedbackCategory {
    val label: String
    val firestoreValue: String
}
