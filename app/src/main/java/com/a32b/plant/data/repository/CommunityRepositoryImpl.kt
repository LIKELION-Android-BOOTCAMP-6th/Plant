package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.source.remote.community.CommunityRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostPage
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val communityRemoteDataSource: CommunityRemoteDataSource,
    private val userRepository: UserRepository
): CommunityRepository {

    private val currentUid: String get() = userRepository.currentUser.value?.uid ?: ""

    override suspend fun loadPostPage(
        cursor: Long?,
        pageSize: Int,
        tagIds: List<String>,
        sharedOnly: Boolean
    ): Result<PostPage> = safeRunCatching {
        val cursorTs = cursor?.toTimestamp()
        val fetched = communityRemoteDataSource.loadPostPage(cursorTs, pageSize, tagIds, sharedOnly)
        val hasMore = fetched.size > pageSize
        val visible = if (hasMore) fetched.take(pageSize) else fetched
        val items = visible.map { it.toDomain(currentUid, emptyList()) }
        val nextCursor = if (hasMore) visible.lastOrNull()?.createdAt.toLong().takeIf { it != 0L } else null
        PostPage(items = items, nextCursor = nextCursor, hasMore = hasMore)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "게시글 페이지 조회 실패", e)
            Result.Failure(handleError(e, "게시글 목록을 불러오지 못했습니다."))
        }
    )

    override suspend fun getAllPosts(
        tagIds: List<String>,
        sharedOnly: Boolean
    ): Result<List<Post>> = safeRunCatching {
        communityRemoteDataSource.loadAllPosts(tagIds, sharedOnly)
            .map { it.toDomain(currentUid, emptyList()) }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "전체 게시글 조회 실패", e)
            Result.Failure(handleError(e, "게시글 검색에 실패했습니다."))
        }
    )

    override suspend fun getPostDetail(postId: String): Result<Post?> = safeRunCatching {
        communityRemoteDataSource.getPostDetail(postId)?.toDomain(currentUid, emptyList())
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "게시글 상세 조회 실패", e)
            Result.Failure(handleError(e, "게시글을 불러오지 못했습니다."))
        }
    )

    override suspend fun getComments(postId: String): Result<List<Comment>> = safeRunCatching {
        communityRemoteDataSource.getComments(postId).map { it.toDomain() }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "댓글 목록 조회 실패", e)
            Result.Failure(handleError(e, "댓글을 불러오지 못했습니다."))
        }
    )

    override suspend fun getTags(): Result<List<Tag>> = safeRunCatching {
        communityRemoteDataSource.getTags().map { it.toDomain() }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "태그 목록 조회 실패", e)
            Result.Failure(handleError(e, "태그를 불러오지 못했습니다."))
        }
    )

    override suspend fun savePost(post: Post, activity: CommunityActivity): Result<String> = safeRunCatching {
        communityRemoteDataSource.savePost(post.toDto(), activity.toDto())
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "게시글 등록 실패", e)
            Result.Failure(handleError(e, "게시글 등록에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.updatePost(isShared, postId, title, content, tag?.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "게시글 수정 실패", e)
            Result.Failure(handleError(e, "게시글 수정에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun deletePost(postId: String, activityId : String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.deletePost(postId, activityId)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "게시글 삭제 실패", e)
            Result.Failure(handleError(e, "게시글 삭제에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean> = safeRunCatching {
        communityRemoteDataSource.toggleLike(postId, uid, title)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Community", "좋아요 처리 실패", e)
            Result.Failure(handleError(e, "좋아요 처리에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.addComment(postId, comment, activity)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "댓글 등록 실패", e)
            Result.Failure(handleError(e, "댓글 등록에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun updateComment(postId: String, commentId: String, newContent: String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.updateComment(postId, commentId, newContent)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "댓글 수정 실패", e)
            Result.Failure(handleError(e, "댓글 수정에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.deleteComment(postId, commentId)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "댓글 삭제 실패", e)
            Result.Failure(handleError(e, "댓글 삭제에 실패했습니다.", isWrite = true))
        }
    )

    override fun observeActivity(selected: String): Flow<List<CommunityActivity>> {
        return communityRemoteDataSource.observeActivity(uid = currentUid, selected)
            .map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun deleteActivity(activityId: String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.deleteActivity(activityId)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Community", "커뮤니티 활동 삭제 실패", e)
            Result.Failure(handleError(e, "오류가 발생했습니다.\n잠시 후 다시 시도해주세요."))

        }
    )


    override suspend fun isPostExist(postId: String): Result<Boolean> = safeRunCatching {
        communityRemoteDataSource.isPostExist(postId)
    }.fold(
        onSuccess = { Result.Success(it)},
        onFailure = { e ->
            Log.e("Community", "게시물 존재 여부 조회 실패", e)
            Result.Failure(handleError(e, "오류가 발생했습니다, \n잠시 후 다시 시도해주세요."))
        }
    )

    override suspend fun findCommentItByActivityId(postId: String, activityId: String): Result<String?> = safeRunCatching {
        communityRemoteDataSource.findCommentIdByActivityId(postId, activityId)
    }.fold(
        onSuccess = { Result.Success(it)},
        onFailure = { e ->
            Log.e("Community", "commentId 조회 실패", e)
            Result.Failure(handleError(e, "알 수 없는 오류가 발생했습니다."))
        }
    )

    private fun handleError(e: Throwable, defaultMessage: String, isWrite: Boolean = false): AppError = when (e) {
        is AppError -> e   // 이미 AppError면 타입 보존 (UnknownUser 등이 Custom으로 뭉개지는 것 방지)
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()

            else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
        }
        is IllegalArgumentException -> AppError.Unknown()
        else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
    }
}
