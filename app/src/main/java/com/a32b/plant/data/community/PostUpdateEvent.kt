package com.a32b.plant.data.community

import android.util.Log
import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.community.PostUpdateObserver
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostUpdateEvent @Inject constructor() : PostUpdateNotifier, PostUpdateObserver {
    // replay=0: 구독 시점 이후의 변경만 수신 (구독 전 과거 이벤트는 silentRefresh가 커버)
    // DROP_OLDEST + 여유 버퍼: 구독자가 아직 없거나 일시적으로 느려도 tryEmit이 실패하지 않도록 함
    // Channel과 달리 여러 구독자가 동시에 같은 이벤트를 모두 받을 수 있어, 추후 다른 화면이 구독을 추가해도 이벤트가 서로 가로채지지 않음
    private val _event = MutableSharedFlow<PostUpdate>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val event: Flow<PostUpdate> = _event.asSharedFlow()
    override fun notifyPostUpdated(update: PostUpdate) {
        val emitted = _event.tryEmit(update)
        if (!emitted) {
            Log.e("PostUpdateEvent", "게시글 변경 이벤트 전송 실패: $update")
        }
    }
}
