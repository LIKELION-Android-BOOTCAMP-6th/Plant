package com.a32b.plant.data.source.remote.block

import com.a32b.plant.data.model.BlockedUserDto
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow

interface BlockRemoteDataSource {
    /** users/{uid}/blocks 구독. 리스너 에러는 Flow 예외로 전달 */
    fun observeBlockedUsers(uid: String): Flow<List<BlockedUserDto>>

    /** 차단 문서 쓰기 시작 — 서버 반영은 호출부가 [Task]로 기다린다 (오프라인이면 캐시에 먼저 반영) */
    fun blockUser(uid: String, blockedUid: String, nickname: String, profileImg: String): Task<Void>

    /** 차단 문서 삭제 시작 */
    fun unblockUser(uid: String, blockedUid: String): Task<Void>
}
