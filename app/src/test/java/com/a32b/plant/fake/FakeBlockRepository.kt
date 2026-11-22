package com.a32b.plant.fake

import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.BlockRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * 테스트 공용 [BlockRepository].
 * 차단·해제가 성공하면 실제 기기 캐시처럼 구독 중인 목록에 바로 반영한다 (Pending도 성공이라 반영).
 * [blockedOf]로 uid별 목록을 미리 넣거나 바꿀 수 있다.
 */
class FakeBlockRepository(
    var blockResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
    var unblockResult: Result<SaveResult> = Result.Success(SaveResult.Saved)
) : BlockRepository {

    private val lists = mutableMapOf<String, MutableStateFlow<List<BlockedUser>>>()

    /** 차단 호출 기록 (내 uid to 대상) */
    val blockCalls = mutableListOf<Pair<String, BlockedUser>>()
    /** 해제 호출 기록 (내 uid to 해제한 uid) */
    val unblockCalls = mutableListOf<Pair<String, String>>()
    /** 구독한 uid 순서 */
    val observedUids = mutableListOf<String>()

    fun blockedOf(uid: String): MutableStateFlow<List<BlockedUser>> =
        lists.getOrPut(uid) { MutableStateFlow(emptyList()) }

    /** false면 구독해도 아무것도 보내지 않는다 (리스너가 첫 목록을 보내기 전에 멈춘 상황) */
    var emitsBlockedUsers: Boolean = true

    override fun observeBlockedUsers(uid: String): Flow<List<BlockedUser>> {
        observedUids += uid
        return if (emitsBlockedUsers) blockedOf(uid) else flow { awaitCancellation() }
    }

    override suspend fun blockUser(uid: String, target: BlockedUser): Result<SaveResult> {
        blockCalls += uid to target
        if (blockResult is Result.Success) {
            val list = blockedOf(uid)
            list.value = listOf(target) + list.value.filterNot { it.uid == target.uid }
        }
        return blockResult
    }

    override suspend fun unblockUser(uid: String, blockedUid: String): Result<SaveResult> {
        unblockCalls += uid to blockedUid
        if (unblockResult is Result.Success) {
            val list = blockedOf(uid)
            list.value = list.value.filterNot { it.uid == blockedUid }
        }
        return unblockResult
    }
}
