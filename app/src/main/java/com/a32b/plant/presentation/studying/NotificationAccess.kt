package com.a32b.plant.presentation.studying

/**
 * 학습 타이머 알림이 알림창·잠금화면에 보일 수 있는 상태인지 (#610)
 */
enum class NotificationAccess {
    /** 알림이 보임 — 안내 없음 */
    ALLOWED,

    /** 13 이상에서 권한을 거절했지만 시스템이 다음에 요청 창을 다시 띄울 수 있음 — 짧게 이유만 안내 */
    DENIED_CAN_ASK_AGAIN,

    /** 요청 창으로는 켤 수 없음 (다시 묻지 않음, 앱 알림 꺼짐, 학습 타이머 채널 꺼짐) — 설정 화면으로 안내 */
    BLOCKED
}

/**
 * 권한 요청 결과와 알림 설정으로 [NotificationAccess]를 정한다
 *
 * @param permissionGranted POST_NOTIFICATIONS 허용 여부. 12 이하는 런타임 권한이 없으므로 true를 넘긴다
 * @param shouldShowRationale 거절 직후의 shouldShowRequestPermissionRationale 값 — true면 첫 거절(다시 요청 가능),
 * false면 다시 묻지 않음 상태(요청 창이 안 뜨고 바로 거절)
 * @param notificationsEnabled 앱 알림 전체가 켜져 있는지 (NotificationManagerCompat.areNotificationsEnabled)
 * @param timerChannelBlocked 학습 타이머 채널이 꺼져 있는지 — 채널이 아직 없으면(서비스를 한 번도 시작하지 않음) false
 */
fun classifyNotificationAccess(
    permissionGranted: Boolean,
    shouldShowRationale: Boolean,
    notificationsEnabled: Boolean,
    timerChannelBlocked: Boolean
): NotificationAccess = when {
    !permissionGranted && shouldShowRationale -> NotificationAccess.DENIED_CAN_ASK_AGAIN
    !permissionGranted -> NotificationAccess.BLOCKED
    !notificationsEnabled || timerChannelBlocked -> NotificationAccess.BLOCKED
    else -> NotificationAccess.ALLOWED
}
