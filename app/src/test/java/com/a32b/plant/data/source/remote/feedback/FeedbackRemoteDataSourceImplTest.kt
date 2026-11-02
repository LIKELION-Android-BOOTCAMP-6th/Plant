package com.a32b.plant.data.source.remote.feedback

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.type.FeedbackType
import com.a32b.plant.domain.type.ImprovementCategory
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackRemoteDataSourceImplTest {
    @Test
    fun bugPayloadHasOnlyRuleKeysAndServerTimestampSentinel() {
        val payload = createFeedback(reproductionSteps = "앱 실행 후 저장 버튼 선택").toFirestoreMap()

        assertEquals(
            setOf(
                "type",
                "reporterId",
                "category",
                "description",
                "appVersion",
                "createdAt",
                "status",
                "reproductionSteps",
                "deviceModel",
                "androidVersion"
            ),
            payload.keys
        )
        assertEquals("bug", payload["type"])
        assertEquals("uid-1", payload["reporterId"])
        assertEquals("ui_bug", payload["category"])
        assertEquals("화면이 멈춥니다.", payload["description"])
        assertEquals("1.0", payload["appVersion"])
        assertEquals("pending", payload["status"])
        assertEquals("앱 실행 후 저장 버튼 선택", payload["reproductionSteps"])
        assertEquals("SM-G965N", payload["deviceModel"])
        assertEquals("28", payload["androidVersion"])
        assertTrue(payload["createdAt"] is FieldValue)
        assertEquals(FieldValue.serverTimestamp(), payload["createdAt"])
        assertFalse(payload["createdAt"] is Timestamp)
    }

    @Test
    fun blankOptionalReproductionStepsAreOmittedFromPayload() {
        val payload = createFeedback(reproductionSteps = null).toFirestoreMap()

        assertFalse(payload.containsKey("reproductionSteps"))
        assertTrue(payload.keys.containsAll(
            setOf("type", "reporterId", "category", "description", "appVersion", "createdAt", "status")
        ))
    }

    @Test
    fun improvementPayloadHasOnlyImprovementRuleKeysAndServerTimestampSentinel() {
        val payload = Feedback.createImprovement(
            reporterId = "uid-1",
            category = ImprovementCategory.UI_IMPROVEMENT,
            description = "화면 구성을 개선해주세요.",
            appVersion = "1.0"
        ).toFirestoreMap()

        assertEquals(
            setOf("type", "reporterId", "category", "description", "appVersion", "createdAt", "status"),
            payload.keys
        )
        assertFalse(payload.containsKey("reproductionSteps"))
        assertFalse(payload.containsKey("deviceModel"))
        assertFalse(payload.containsKey("androidVersion"))
        assertEquals("improvement", payload["type"])
        assertEquals("ui_improvement", payload["category"])
        assertEquals("pending", payload["status"])
        assertTrue(payload["createdAt"] is FieldValue)
        assertEquals(FieldValue.serverTimestamp(), payload["createdAt"])
        assertFalse(payload["createdAt"] is Timestamp)
        assertEquals(setOf("bug", "improvement"), FeedbackType.entries.map { it.firestoreKey }.toSet())
        assertEquals(
            setOf("ui_bug", "feature_error", "other"),
            BugCategory.entries.map { it.firestoreKey }.toSet()
        )
        assertEquals(
            setOf("ui_improvement", "new_feature", "convenience", "other"),
            ImprovementCategory.entries.map { it.firestoreKey }.toSet()
        )
    }

    private fun createFeedback(reproductionSteps: String?) = Feedback.createBug(
        reporterId = "uid-1",
        category = BugCategory.UI_BUG,
        description = "화면이 멈춥니다.",
        reproductionSteps = reproductionSteps,
        deviceModel = "SM-G965N",
        androidVersion = "28",
        appVersion = "1.0"
    )
}
