package com.a32b.plant.data.session

import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredObserver
import com.a32b.plant.domain.session.SessionExpiredReason
import android.util.Log
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
        val result = _channel.trySend(reason)
        if (result.isFailure) {
            Log.e("SessionExpiredEvent", "세션 만료 이벤트 전송 실패: $reason")
        }
    }
}
