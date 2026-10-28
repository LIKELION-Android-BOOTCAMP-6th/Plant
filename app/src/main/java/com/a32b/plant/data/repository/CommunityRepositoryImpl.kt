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
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
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

    override suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit> {
        safeRunCatching {
            communityRemoteDataSource.updatePost(isShared, postId, title, content, tag?.toDto())
        }.onFailure { e ->
            Log.e("Community", "게시글 수정 실패", e)
            return Result.Failure(handleError(e, "게시글 수정에 실패했습니다.", isWrite = true))
        }

        safeRunCatching { communityRemoteDataSource.updateActivityTitle(postId, title) }
            .onFailure { Log.e("Community", "activity title 업데이트 실패 (게시글은 수정됨): postId=$postId", it) }

        return Result.Success(Unit)
    }

    override suspend fun deletePost(postId: String, activityId: String): Result<Unit> {
        val commentActivityIds = safeRunCatching {
            communityRemoteDataSource.deletePost(postId)
        }.fold(
            onSuccess = { it },
            onFailure = { e ->
                Log.e("Community", "게시글 삭제 실패", e)
                return Result.Failure(handleError(e, "게시글 삭제에 실패했습니다.", isWrite = true))
            }
        )

        commentActivityIds.forEach { commentActivityId ->
            safeRunCatching { communityRemoteDataSource.deleteActivity(commentActivityId) }
                .onFailure { Log.e("Community", "댓글 activity 삭제 실패 (게시글은 삭제됨): $commentActivityId", it) }
        }

        if (activityId.isNotEmpty()) {
            safeRunCatching { communityRemoteDataSource.deleteActivity(activityId) }
                .onFailure { Log.e("Community", "activity 삭제 실패 (게시글은 삭제됨): $activityId", it) }
        }

        return Result.Success(Unit)
    }

    override suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean> {
        val wasLiked = safeRunCatching {
            communityRemoteDataSource.toggleLike(postId, uid)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("Community", "좋아요 처리 실패", e)
                return Result.Failure(handleError(e, "좋아요 처리에 실패했습니다.", isWrite = true))
            }
        ).let { (it as Result.Success).data }

        if (wasLiked) {
            safeRunCatching { communityRemoteDataSource.deleteActivityByType(uid, postId, ActivityType.LIKE) }
                .onFailure { Log.e("Community", "좋아요 activity 삭제 실패 (좋아요는 취소됨): postId=$postId", it) }
        } else {
            safeRunCatching { communityRemoteDataSource.createActivity(CommunityActivity.like(uid = uid, title = title, targetId = postId)) }
                .onFailure { Log.e("Community", "좋아요 activity 생성 실패 (좋아요는 추가됨): postId=$postId", it) }
        }

        return Result.Success(wasLiked)
    }

    override suspend fun toggleBookmark(postId: String, uid: String, title: String): Result<Boolean> {
        val wasBookmarked = safeRunCatching {
            communityRemoteDataSource.toggleBookmark(postId, uid)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("Community", "북마크 처리 실패", e)
                return Result.Failure(handleError(e, "북마크 처리에 실패했습니다.", isWrite = true))
            }
        ).let { (it as Result.Success).data }

        if (wasBookmarked) {
            safeRunCatching { communityRemoteDataSource.deleteActivityByType(uid, postId, ActivityType.BOOKMARK) }
                .onFailure { Log.e("Community", "북마크 activity 삭제 실패 (북마크는 취소됨): postId=$postId", it) }
        } else {
            safeRunCatching { communityRemoteDataSource.createActivity(CommunityActivity.bookmark(uid = uid, title = title, targetId = postId)) }
                .onFailure { Log.e("Community", "북마크 activity 생성 실패 (북마크는 추가됨): postId=$postId", it) }
        }

        return Result.Success(wasBookmarked)
    }

    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.addComment(postId, comment, activity)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "댓글 등록 실패", e)
            Result.Failure(handleError(e, "댓글 등록에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun updateComment(postId: String, commentId: String, newContent: String, activityId: String): Result<Unit> {
        val hasActivityId = activityId.isNotBlank()
        // 모든 댓글은 activityId를 가진다는 전제 — 깨지면 '내 활동' 문구가 갱신되지 않으므로 원인 추적용으로 남김
        if (!hasActivityId) Log.w("Community", "activityId가 비어 있어 댓글 본문만 수정: commentId=$commentId")

        // 주의: 아래 호출 전에 suspend 지점을 추가하면 화면 이탈(코루틴 취소) 시 batch가 SDK에 적재되지 못한다
        var result = safeRunCatching {
            communityRemoteDataSource.updateComment(postId, commentId, newContent, activityId.takeIf { hasActivityId })
        }

        // activity 문서가 이미 없으면 batch 전체가 실패한다. 보안 규칙이 resource.data를 참조하므로
        // NOT_FOUND가 아니라 PERMISSION_DENIED로 거절될 수 있어 둘 다 보정 대상 — 댓글 본문만이라도 수정되도록 재시도
        // (진짜 권한 없음이면 재시도도 같은 이유로 실패해 그대로 에러가 반환됨)
        // 한계: 오프라인에서 저장하고 화면을 이탈한 뒤 연결이 복구되면 서버가 batch를 거절하는데,
        //       이때는 코루틴이 이미 사라져 이 재시도가 실행되지 않아 수정이 반영되지 않는다 (activity 문서가 없는 댓글에 한함)
        if (hasActivityId && result.exceptionOrNull().isMissingOrDenied()) {
            Log.w("Community", "수정할 activity가 없어 댓글 본문만 수정: commentId=$commentId, activityId=$activityId")
            result = safeRunCatching {
                communityRemoteDataSource.updateComment(postId, commentId, newContent, null)
            }
        }

        result.onFailure { e ->
            Log.e("Community", "댓글 수정 실패", e)
            return Result.Failure(handleError(e, "댓글 수정에 실패했습니다.", isWrite = true))
        }

        return Result.Success(Unit)
    }

    private fun Throwable?.isMissingOrDenied() =
        this is FirebaseFirestoreException && (
            code == FirebaseFirestoreException.Code.NOT_FOUND ||
                code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )

    override suspend fun deleteComment(postId: String, commentId: String): Result<Boolean> {
        var result = safeRunCatching {
            communityRemoteDataSource.deleteComment(postId, commentId, deleteActivity = true)
        }

        // 지울 activity 문서가 이미 없으면 보안 규칙(resource.data 참조)에 걸려 트랜잭션 전체가 거절될 수 있음
        // → 댓글 삭제가 막히지 않도록 activity 없이 한 번 더 시도 (진짜 권한 없음이면 재시도도 실패해 에러 반환)
        if (result.exceptionOrNull().isMissingOrDenied()) {
            Log.w("Community", "activity 삭제 포함 트랜잭션 실패 — 댓글만 삭제 재시도: commentId=$commentId", result.exceptionOrNull())
            result = safeRunCatching {
                communityRemoteDataSource.deleteComment(postId, commentId, deleteActivity = false)
            }
        }

        return result.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("Community", "댓글 삭제 실패", e)
                Result.Failure(handleError(e, "댓글 삭제에 실패했습니다.", isWrite = true))
            }
        )
    }

    override fun observeActivity(selected: String): Flow<List<CommunityActivity>> {
        if (currentUid.isEmpty()) {
            Log.e("CommunityRepository", "UID가 비어있어 활동 구독을 시작하지 않습니다.")
            return flowOf(emptyList())
        }
        return communityRemoteDataSource.observeActivity(uid = currentUid, selected)
            .map { dtos -> dtos.map { it.toDomain() } }
            .catch { e -> Log.e("CommunityRepository", "활동 구독 실패: ${e.message}", e) }
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
