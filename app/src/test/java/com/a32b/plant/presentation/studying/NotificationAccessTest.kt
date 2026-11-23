package com.a32b.plant.presentation.studying

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 권한 요청 결과·알림 설정으로 안내 종류를 정하는지 확인 (#610)
 */
class NotificationAccessTest {

    private fun classify(
        permissionGranted: Boolean = true,
        shouldShowRationale: Boolean = false,
        notificationsEnabled: Boolean = true,
        timerChannelBlocked: Boolean = false
    ) = classifyNotificationAccess(permissionGranted, shouldShowRationale, notificationsEnabled, timerChannelBlocked)

    @Test
    fun `allowed when permission granted and notifications on`() {
        assertEquals(NotificationAccess.ALLOWED, classify())
    }

    @Test
    fun `first denial can ask again`() {
        assertEquals(
            NotificationAccess.DENIED_CAN_ASK_AGAIN,
            classify(permissionGranted = false, shouldShowRationale = true, notificationsEnabled = false)
        )
    }

    @Test
    fun `denied without rationale is blocked`() {
        assertEquals(
            NotificationAccess.BLOCKED,
            classify(permissionGranted = false, shouldShowRationale = false, notificationsEnabled = false)
        )
    }

    @Test
    fun `blocked when app notifications are off on 12 or lower`() {
        assertEquals(NotificationAccess.BLOCKED, classify(notificationsEnabled = false))
    }

    @Test
    fun `blocked when only the timer channel is off`() {
        assertEquals(NotificationAccess.BLOCKED, classify(timerChannelBlocked = true))
    }
}
