package com.a32b.plant.domain.session

interface SessionExpiredNotifier {
    fun notifySessionExpired(reason: SessionExpiredReason = SessionExpiredReason.USER_NOT_FOUND)
}
