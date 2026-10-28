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
        // 좋아요/북마크는 (유형, 게시글) 단위로 묶어 한 번만 해제 — removeXXX 한 번이 그 게시글의 같은 유형 activity를 모두 지우므로,
        // 중복 activity를 각각 처리하면 같은 문서를 동시에 지우다 뒤쪽이 보안 규칙(resource.data 참조)에 거절되어 거짓 실패가 집계됨
        // (유형까지 키에 넣어야 같은 게시글의 좋아요와 북마크가 한 그룹으로 묶이지 않음)
        val (reactions, others) = activities.partition { it.type == ActivityType.LIKE || it.type == ActivityType.BOOKMARK }
        val groups = others.map { listOf(it) } + reactions.groupBy { it.type to it.targetId }.values

        // 그룹마다 activity 개수만큼 Result를 돌려받아, 실패 개수를 activity 기준으로 집계
        val results = groups.map { group -> async { deleteGroup(group) } }.awaitAll().flatten()

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

    /** 같은 게시글을 대상으로 하는 activity 묶음을 삭제하고, 묶음 안 activity 하나당 Result 하나를 반환 */
    private suspend fun deleteGroup(group: List<CommunityActivity>): List<Result<Unit>> {
        val activity = group.first()

        val exists = when (val existsResult = repository.isPostExist(activity.targetId)) {
            is Result.Success -> existsResult.data
            is Result.Failure -> return group.map { existsResult }
        }
        // 게시글이 없으면 activity id로 각각 지움 — 서로 다른 문서라 충돌이 없고, 하나만 지우면 나머지가 목록에 남음
        if (!exists) return group.map { repository.deleteActivity(it.id) }

        // 모든 분기가 Result를 반환해야 결과에서 실패가 집계됨
        // (when에 else가 없으면 문장으로 취급되어 값이 Unit이 되고, 실패가 "삭제되었습니다."로 묻힘)
        val result = when (activity.type) {
            ActivityType.POST -> repository.deletePost(activity.targetId, activity.id)
            ActivityType.COMMENT -> deleteComment(activity)

            // 사용자 없음(null)도 실패로 집계 — 세션 만료 알림은 ensureCurrentUserUseCase 내부에서 이미 발송됨
            // 토글이 아닌 해제(remove)를 호출 — 이미 해제된 항목을 지워도 다시 좋아요/북마크되지 않음
            ActivityType.LIKE ->
                ensureCurrentUserUseCase { user ->
                    repository.removeLike(activity.targetId, user.uid)
                } ?: Result.Failure(AppError.UnknownUser())

            ActivityType.BOOKMARK ->
                ensureCurrentUserUseCase { user ->
                    repository.removeBookmark(activity.targetId, user.uid)
                } ?: Result.Failure(AppError.UnknownUser())

            else -> Result.Failure(AppError.Unknown("알 수 없는 활동 유형입니다: ${activity.type}"))
        }
        // 여러 개로 묶이는 건 좋아요/북마크뿐 — 한 번의 해제 결과가 묶음 안 activity 전부에 해당
        return group.map { result }
    }

    private suspend fun deleteComment(activity: CommunityActivity): Result<Unit> {
        val cId = activity.commentId ?: when (
            val r = repository.findCommentItByActivityId(activity.targetId, activity.id)
        ) {
            is Result.Success -> r.data
            is Result.Failure -> return r
        }
        //조회 실패든 뭐든 commentId가 널이라면 삭제 안 함 (실패로 집계하지 않음)
        if (cId == null) return Result.Success(Unit)

        return when (val r = repository.deleteComment(activity.targetId, cId)) {
            // 댓글이 이미 없던 경우(false) — 이전 버그 등으로 activity만 남은 고아 데이터이므로 직접 정리
            // (댓글 조회가 null인 위 경우는 오프라인 캐시 오판 위험이 있어 기존대로 삭제하지 않음)
            is Result.Success -> if (r.data) Result.Success(Unit) else repository.deleteOrphanedActivity(activity.id)
            is Result.Failure -> r
        }
    }
}
