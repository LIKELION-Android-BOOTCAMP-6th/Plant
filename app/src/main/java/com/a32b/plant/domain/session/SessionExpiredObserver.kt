package com.a32b.plant.domain.session

import kotlinx.coroutines.flow.SharedFlow

interface SessionExpiredObserver {
    val event: SharedFlow<Unit>
}
