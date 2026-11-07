package com.a32b.plant.data.mapper

import com.a32b.plant.domain.model.Feedback
import com.google.firebase.firestore.FieldValue

fun Feedback.toFirestoreMap(): Map<String, Any> = buildMap {
    put("type", type.firestoreKey)
    put("reporterId", reporterId)
    put("category", category.firestoreKey)
    put("description", description)
    put("appVersion", appVersion)
    put("createdAt", FieldValue.serverTimestamp())
    put("status", "pending")
    reproductionSteps?.let { put("reproductionSteps", it) }
    deviceModel?.let { put("deviceModel", it) }
    androidVersion?.let { put("androidVersion", it) }
}
