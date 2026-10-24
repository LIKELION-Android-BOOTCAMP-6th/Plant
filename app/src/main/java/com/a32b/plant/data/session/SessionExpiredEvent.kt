package com.a32b.plant.data.session

import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredObserver
import com.a32b.plant.domain.session.SessionExpiredReason
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiredEvent @Inject constructor() : SessionExpiredNotifier, SessionExpiredObserver {
    private val _channel = Channel<SessionExpiredReason>(Channel.BUFFERED)
    override val event: Flow<SessionExpiredReason> = _channel.receiveAsFlow()
    override fun notifySessionExpired(reason: SessionExpiredReason) {
        _channel.trySend(reason)
    }
}
