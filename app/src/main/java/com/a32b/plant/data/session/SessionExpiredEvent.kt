package com.a32b.plant.data.session

import com.a32b.plant.domain.session.SessionExpiredNotifier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionExpiredEvent @Inject constructor() : SessionExpiredNotifier{
    private val _event = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val event : SharedFlow<Unit> = _event.asSharedFlow()
    override fun notifySessionExpired() {
        _event.tryEmit(Unit)
    }
}
