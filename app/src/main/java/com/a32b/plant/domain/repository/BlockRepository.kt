package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

/** 커뮤니티 사용자 차단 (#660) */
interface BlockRepository {

    /**
     * [uid]의 차단 목록 구독 — 최근에 차단한 순서.
     * 같은 uid는 화면이 여러 개여도 리스너 하나를 함께 쓰고, 마지막 목록을 들고 있어 새로 구독하면 바로 받는다.
     * 첫 목록을 받기 전에는 아무것도 보내지 않는다(아래 실패 경우 제외). 예외는 보내지 않는다:
     * 리스너가 실패하면 이 uid의 목록을 받은 적이 있으면 그대로 유지하고, 받은 적이 없으면 빈 목록을 한 번 보낸다.
     * 실패한 뒤에는 구독하는 화면이 남아 있는 동안 다시 연결하지 않는다 (구독하는 화면이 모두 백스택에서 빠지거나 로그아웃으로 구독이 끊긴 뒤 다시 구독하면 연결).
     */
    fun observeBlockedUsers(uid: String): Flow<List<BlockedUser>>

    /**
     * [uid]가 [target]을 차단한다. 이미 차단한 사용자면 같은 문서에 덮어써 성공한다.
     * 오프라인이면 기기 캐시에 바로 반영되고(구독 중인 목록에서도 바로 숨겨짐) [SaveResult.Pending]을 돌려준다.
     */
    suspend fun blockUser(uid: String, target: BlockedUser): Result<SaveResult>

    /** [uid]의 차단 목록에서 [blockedUid]를 뺀다. 오프라인이면 [SaveResult.Pending] */
    suspend fun unblockUser(uid: String, blockedUid: String): Result<SaveResult>
}
