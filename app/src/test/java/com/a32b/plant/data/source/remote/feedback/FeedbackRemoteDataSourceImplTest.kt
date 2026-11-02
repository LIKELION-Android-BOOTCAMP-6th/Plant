package com.a32b.plant.data.source.remote.feedback

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.type.BugCategory
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