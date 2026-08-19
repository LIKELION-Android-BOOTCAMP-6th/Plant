package com.a32b.plant.domain.usecase.community

import android.util.Log
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.google.android.play.integrity.internal.ac
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class DeleteActivitiesUseCase @Inject constructor(
    private val repository: CommunityRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(activities: List<CommunityActivity>): Result<Unit> = coroutineScope{
        val results = activities.map { activity ->
            async {
                when( val existsResult = repository.isPostExist(activity.targetId)){
                    is Result.Failure -> Result.Failure(existsResult.error)
                    is Result.Success -> {
                        if (existsResult.data){
                            when(activity.type){
                                ActivityType.POST -> repository.deletePost(activity.targetId, activity.id)
                                ActivityType.COMMENT -> {
                                    val cId = activity.commentId ?: when(
                                        val r = repository.findCommentItByActivityId(activity.targetId, activity.id)
                                    ){
                                        is Result.Success -> r.data
                                        is Result.Failure -> return@async Result.Failure(r.error)
                                    }
                                    //조회 실패든 뭐든 commentId가 널이라면 삭제 안 함
                                    cId?.let {
                                        repository.deleteComment(activity.targetId, it)
                                    }

                                }

                                ActivityType.LIKE ->
                                    ensureCurrentUserUseCase{ user ->
                                        repository.toggleLike(activity.targetId, user.uid, activity.title)
                                    }
                            }
                        } else {
                            repository.deleteActivity(activity.id)
                        }
                    }
                }
            }
        }.awaitAll()

        val failures = results.filterIsInstance<Result.Failure>()

        if (failures.isEmpty()) {
            Result.Success(Unit)
        } else {
            Log.e("Community", "활동 삭제 실패 ${failures.size}건: ${failures.map { it.error }}")
            Result.Failure(
                AppError.Unknown(
                    message = "${activities.size}개 중 ${failures.size}개 삭제에 실패했습니다."
                )
            )
        }
    }
}
