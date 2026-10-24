package com.a32b.plant.domain.session

import kotlinx.coroutines.flow.Flow

interface SessionExpiredObserver {
    val event: Flow<SessionExpiredReason>
}
