package com.a32b.plant.presentation.community

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.Post

/**
 * 차단한 사용자의 게시글·댓글을 화면에서 빼는 기준 (#660)
 *
 * 서버 쿼리로 거르지 않고 받은 뒤 화면에서 거른다.
 * 게시글 목록 쿼리가 태그 필터에 whereIn("tag.id")을 쓰는데, Firestore는 in과 not-in을 한 쿼리에 같이 쓸 수 없고
 * not-in은 값 10개까지라 차단 목록이 길어지면 쓸 수 없다.
 * 거르는 기준은 uid라 차단한 사용자가 닉네임을 바꿔도 계속 숨겨진다.
 */
object BlockedContentFilter {

    fun isBlocked(post: Post, blockedUids: Set<String>): Boolean = post.author.id in blockedUids

    fun visiblePosts(posts: List<Post>, blockedUids: Set<String>): List<Post> =
        if (blockedUids.isEmpty()) posts else posts.filterNot { isBlocked(it, blockedUids) }

    fun visibleComments(comments: List<Comment>, blockedUids: Set<String>): List<Comment> =
        if (blockedUids.isEmpty()) comments else comments.filterNot { it.user.uid in blockedUids }

    /**
     * [posts]가 전부 차단한 사용자 글이라 화면에 보일 글이 하나도 없는데 다음 페이지가 있는지.
     * 목록은 마지막 글이 보일 때 다음 페이지를 부르는데, 보일 글이 없으면 그 신호가 오지 않으므로 직접 불러야 한다.
     * 빈 목록은 false — 받은 글이 없으면 다음 페이지도 없는 것으로 본다 (무한 반복 방지)
     */
    fun needsNextPage(posts: List<Post>, hasMore: Boolean, blockedUids: Set<String>): Boolean =
        hasMore && posts.isNotEmpty() && posts.all { isBlocked(it, blockedUids) }
}
