package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
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
                repository.isPostExist(activity.targetId)
                    .onSuccess {
                        if (it){
                            when(activity.type){
                                ActivityType.POST -> repository.deletePost(activity.targetId, activity.id)
                                ActivityType.COMMENT -> repository.deleteComment(activity.targetId, activity.commentId!!)
                                ActivityType.LIKE -> {
                                    ensureCurrentUserUseCase{ user ->
                                        repository.toggleLike(activity.targetId, user.uid, activity.title)
                                    }
                                }
                            }
                        }
                        else repository.deleteActivity(activity.id)
                    }
                    .onFailure { error ->
                        Result.Failure(error)
                    }
            }
        }.awaitAll()

        (results.firstOrNull { it is Result.Failure } ?: Result.Success(Unit)) as Result<Unit>
    }
}
