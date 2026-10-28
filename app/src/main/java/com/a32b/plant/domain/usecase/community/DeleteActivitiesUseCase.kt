package com.a32b.plant.domain.usecase.community

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
                // 모든 분기가 Result를 반환해야 awaitAll() 결과에서 실패가 집계됨
                // (when에 else가 없으면 문장으로 취급되어 값이 Unit이 되고, 실패가 "삭제되었습니다."로 묻힘)
                when( val existsResult = repository.isPostExist(activity.targetId)){
                    is Result.Failure -> existsResult
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
                                    //조회 실패든 뭐든 commentId가 널이라면 삭제 안 함 (실패로 집계하지 않음)
                                    if (cId == null) {
                                        Result.Success(Unit)
                                    } else {
                                        when (val r = repository.deleteComment(activity.targetId, cId)) {
                                            // 댓글이 이미 없던 경우(false) — 이전 버그 등으로 activity만 남은 고아 데이터이므로 직접 정리
                                            // (댓글 조회가 null인 위 경우는 오프라인 캐시 오판 위험이 있어 기존대로 삭제하지 않음)
                                            is Result.Success -> if (r.data) Result.Success(Unit) else repository.deleteOrphanedActivity(activity.id)
                                            is Result.Failure -> r
                                        }
                                    }
                                }

                                // 사용자 없음(null)도 실패로 집계 — 세션 만료 알림은 ensureCurrentUserUseCase 내부에서 이미 발송됨
                                ActivityType.LIKE ->
                                    ensureCurrentUserUseCase{ user ->
                                        repository.toggleLike(activity.targetId, user.uid, activity.title)
                                    } ?: Result.Failure(AppError.UnknownUser())

                                ActivityType.BOOKMARK ->
                                    ensureCurrentUserUseCase { user ->
                                        repository.toggleBookmark(activity.targetId, user.uid, activity.title)
                                    } ?: Result.Failure(AppError.UnknownUser())

                                else -> Result.Failure(AppError.Unknown("알 수 없는 활동 유형입니다: ${activity.type}"))
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
        } else if (failures.any { it.error is AppError.UnknownUser }) {
            // 세션 만료 — 알림은 이미 발송됐으므로 삭제 실패 토스트 없이 UnknownUser만 전달
            Result.Failure(AppError.UnknownUser())
        } else {
            // 실패 로그는 각 Repository 메서드에서 이미 남김 (Domain 레이어는 Android 의존 없이 유지)
            Result.Failure(
                AppError.Unknown(
                    message = "${activities.size}개 중 ${failures.size}개 삭제에 실패했습니다."
                )
            )
        }
    }
}
