package com.a32b.plant.fake

import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier

/** 테스트 공용 [PostUpdateNotifier]. 보낸 신호를 종류별로 기록한다. */
class RecordingPostUpdateNotifier : PostUpdateNotifier {
    val updates = mutableListOf<PostUpdate>()
    val created = mutableListOf<String>()
    val deleted = mutableListOf<String>()
    override fun notifyPostUpdated(update: PostUpdate) { updates += update }
    override fun notifyPostCreated(postId: String) { created += postId }
    override fun notifyPostDeleted(postId: String) { deleted += postId }
}
