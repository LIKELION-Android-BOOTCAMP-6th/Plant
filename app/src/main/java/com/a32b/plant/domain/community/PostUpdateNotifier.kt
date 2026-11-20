package com.a32b.plant.domain.community

interface PostUpdateNotifier {
    fun notifyPostUpdated(update: PostUpdate)

    // 새 글은 목록에 아직 없어 PostUpdate로 패치할 수 없다. 목록이 다음 복귀 때 다시 불러오도록 알리기만 한다 (#669)
    fun notifyPostCreated(postId: String)
}
