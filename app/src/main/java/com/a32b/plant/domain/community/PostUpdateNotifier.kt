package com.a32b.plant.domain.community

interface PostUpdateNotifier {
    fun notifyPostUpdated(update: PostUpdate)

    // 새 글은 목록에 아직 없어 PostUpdate로 패치할 수 없다. 목록이 다음 복귀 때 다시 불러오도록 알리기만 한다 (#669)
    fun notifyPostCreated(postId: String)

    // 삭제는 PostUpdate의 부분 패치로 나타낼 수 없어 따로 알린다. 목록은 이 글을 바로 빼고 다음 복귀 때 다시 불러온다 (#712)
    fun notifyPostDeleted(postId: String)
}
