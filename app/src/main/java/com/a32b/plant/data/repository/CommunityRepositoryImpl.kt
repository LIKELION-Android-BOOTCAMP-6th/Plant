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
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
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

    override fun getPostList(): Flow<List<Post>> {
        return communityRemoteDataSource.getPostList()
            .map { dtos -> dtos.map { it.toDomain(currentUid, emptyList()) } }
    }

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

    override suspend fun deletePost(postId: String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.deletePost(postId)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Community", "게시글 삭제 실패", e)
            Result.Failure(handleError(e, "게시글 삭제에 실패했습니다.", isWrite = true))
        }
    )

    override suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean, title: String): Result<Unit> = safeRunCatching {
        communityRemoteDataSource.toggleLike(postId, uid, isAlreadyLiked, title)
    }.fold(
        onSuccess = { Result.Success(Unit) },
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

    private fun handleError(e: Throwable, defaultMessage: String, isWrite: Boolean = false): AppError = when (e) {
        is AppError -> e   // 이미 AppError면 타입 보존 (UnknownUser 등이 Custom으로 뭉개지는 것 방지)
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()

            else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
        }
        else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
    }
}
