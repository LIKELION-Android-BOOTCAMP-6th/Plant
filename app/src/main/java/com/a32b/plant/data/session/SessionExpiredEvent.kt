package com.a32b.plant.data.session

import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredObserver
import com.a32b.plant.domain.session.SessionExpiredReason
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiredEvent @Inject constructor() : SessionExpiredNotifier, SessionExpiredObserver {
    private val _event = MutableSharedFlow<SessionExpiredReason>(extraBufferCapacity = 1)
    override val event: SharedFlow<SessionExpiredReason> = _event.asSharedFlow()
    override fun notifySessionExpired(reason: SessionExpiredReason) {
        _event.tryEmit(reason)
    }
}
