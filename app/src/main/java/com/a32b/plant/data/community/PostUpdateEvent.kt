package com.a32b.plant.data.community

import android.util.Log
import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.community.PostUpdateObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostUpdateEvent @Inject constructor() : PostUpdateNotifier, PostUpdateObserver {
    private val _channel = Channel<PostUpdate>(Channel.BUFFERED)
    override val event: Flow<PostUpdate> = _channel.receiveAsFlow()
    override fun notifyPostUpdated(update: PostUpdate) {
        val result = _channel.trySend(update)
        if (result.isFailure) {
            Log.e("PostUpdateEvent", "게시글 변경 이벤트 전송 실패: $update")
        }
    }
}
