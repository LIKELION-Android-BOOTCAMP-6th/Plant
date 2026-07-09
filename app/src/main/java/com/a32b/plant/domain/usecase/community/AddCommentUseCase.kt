package com.a32b.plant.domain.usecase.community

import android.net.http.NetworkException
import android.util.Log
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import javax.inject.Inject


class AddCommentUseCase @Inject constructor(
    private val auth : AuthRepository,
    private val community : CommunityRepository,
    private val firebaseAuth: FirebaseAuth
) {

    suspend operator fun invoke(postId: String, title: String, content: String ) : Result<Unit> = runCatching {
        community.addComment(
            postId = postId,
            comment = Comment(
                commentId = "",
                user = CommentUser(CurrentUser.uid, CurrentUser.nickname, CurrentUser.profileImg),
                content = content,
                activityId = "",
                createdAt = null
            ),
            activity = CommunityActivity.comment(
                uid = CurrentUser.uid, title = title, targetId = postId, content = content, commentId = ""
            )
        )
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            if (e is CancellationException) throw e

            e.printStackTrace()

            val error = when (e){
                is FirebaseNetworkException -> AppError.Network()
                else -> AppError.Custom("댓글 등록 실패")
            }

            Log.e("댓글 등록 유즈케이스", e.message.toString())
            Result.Failure(error)
        }
    )

}