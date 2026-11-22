package com.a32b.plant.presentation.community

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
import com.a32b.plant.domain.model.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 커뮤니티 목록·상세가 차단한 사용자 글을 거르는 기준과 빈 페이지 자동 추가 로드 판단 (#660)
 * CommunityListViewModel은 받은 페이지마다 [BlockedContentFilter.needsNextPage]로 다음 페이지를 이어 부를지 정하고,
 * 화면에는 [BlockedContentFilter.visiblePosts]로 거른 목록을 보낸다.
 */
class BlockedContentFilterTest {

    private val blocked = setOf("bad")

    @Test
    fun `차단한 사용자의 글만 빼고 순서는 유지한다`() {
        val posts = listOf(post("1", "ok"), post("2", "bad"), post("3", "other"))
        assertEquals(listOf("1", "3"), BlockedContentFilter.visiblePosts(posts, blocked).map { it.postId })
    }

    @Test
    fun `차단 목록이 비어 있으면 그대로 돌려준다`() {
        val posts = listOf(post("1", "bad"))
        assertEquals(posts, BlockedContentFilter.visiblePosts(posts, emptySet()))
    }

    @Test
    fun `닉네임이 바뀌어도 uid 기준이라 계속 숨긴다`() {
        val renamed = post("1", "bad").copy(author = PostAuthor(id = "bad", nickname = "새 닉네임", profileImg = "1"))
        assertTrue(BlockedContentFilter.visiblePosts(listOf(renamed), blocked).isEmpty())
    }

    @Test
    fun `차단한 사용자의 댓글만 뺀다 (임시 댓글 포함 다른 댓글은 유지)`() {
        val comments = listOf(
            comment("c1", "bad"),
            comment("c2", "me"),
            Comment.createPending(CommentUser("me", "나", "0"), "등록 중", 0L)
        )
        val visible = BlockedContentFilter.visibleComments(comments, blocked)
        assertEquals(listOf("me", "me"), visible.map { it.user.uid })
    }

    @Test
    fun `받은 페이지가 전부 차단한 사용자 글이고 다음 페이지가 있으면 이어서 부른다`() {
        val page = listOf(post("1", "bad"), post("2", "bad"))
        assertTrue(BlockedContentFilter.needsNextPage(page, hasMore = true, blockedUids = blocked))
    }

    @Test
    fun `마지막 페이지면 이어 부르지 않는다 (빈 목록 안내)`() {
        val page = listOf(post("1", "bad"))
        assertFalse(BlockedContentFilter.needsNextPage(page, hasMore = false, blockedUids = blocked))
    }

    @Test
    fun `보일 글이 하나라도 있으면 이어 부르지 않는다 (화면 하단 도달 신호에 맡김)`() {
        val page = listOf(post("1", "bad"), post("2", "ok"))
        assertFalse(BlockedContentFilter.needsNextPage(page, hasMore = true, blockedUids = blocked))
    }

    @Test
    fun `빈 페이지는 이어 부르지 않는다 (무한 반복 방지)`() {
        assertFalse(BlockedContentFilter.needsNextPage(emptyList(), hasMore = true, blockedUids = blocked))
    }

    @Test
    fun `차단한 사용자가 없으면 이어 부르지 않는다`() {
        val page = listOf(post("1", "bad"))
        assertFalse(BlockedContentFilter.needsNextPage(page, hasMore = true, blockedUids = emptySet()))
    }

    private fun post(id: String, authorId: String) =
        Post.createOriginal(
            author = PostAuthor(id = authorId, nickname = "닉네임$authorId", profileImg = "1"),
            title = "제목$id",
            content = "내용",
            tag = Tag(id = "tag1", name = "공부")
        ).copy(postId = id)

    private fun comment(id: String, uid: String) = Comment(
        commentId = id,
        user = CommentUser(uid, "닉네임$uid", "1"),
        content = "댓글",
        activityId = "",
        createdAt = 0L,
        reportCount = 0,
        isHidden = false,
        isPending = false
    )
}
