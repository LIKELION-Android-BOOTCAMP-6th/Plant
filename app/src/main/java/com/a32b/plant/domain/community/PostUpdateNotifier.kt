package com.a32b.plant.domain.community

interface PostUpdateNotifier {
    fun notifyPostUpdated(update: PostUpdate)
}
