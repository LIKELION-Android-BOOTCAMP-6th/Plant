package com.a32b.plant.domain.usecase.community

import android.util.Log
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
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
                                    // return@async: 바깥 when(activity.type)에 else가 없어 값이 Unit이 되므로, 결과를 직접 반환해야 실패가 집계됨
                                    cId?.let {
                                        return@async when (val r = repository.deleteComment(activity.targetId, it)) {
                                            // 댓글이 이미 없던 경우(false) — 이전 버그 등으로 activity만 남은 고아 데이터이므로 직접 정리
                                            // (댓글 조회가 null인 위 경우는 오프라인 캐시 오판 위험이 있어 기존대로 삭제하지 않음)
                                            is Result.Success -> if (r.data) Result.Success(Unit) else repository.deleteOrphanedActivity(activity.id)
                                            is Result.Failure -> r
                                        }
                                    }

                                }

                                ActivityType.LIKE ->
                                    ensureCurrentUserUseCase{ user ->
                                        repository.toggleLike(activity.targetId, user.uid, activity.title)
                                    }

                                ActivityType.BOOKMARK ->
                                    ensureCurrentUserUseCase { user ->
                                        repository.toggleBookmark(activity.targetId, user.uid, activity.title)
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
