package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow

interface ItemRepository {
    /**
     * 로그인한 사용자의 박스를 연다. 서버가 추첨·지급하고 뽑힌 목록을 돌려준다 (#454)
     * 사용자는 서버가 로그인 토큰으로 확인하므로 uid를 받지 않는다. 같은 requestId는 서버에서 한 번만 처리된다
     */
    suspend fun openBonusBoxes(boxCount: Int, requestId: String) : Result<List<ItemType>>
    fun getBoxCount(uid: String): Flow<Int>
    suspend fun consumeItems(uid: String, items: Map<ItemType, Int>): Result<Unit>
    suspend fun canConsumeItems(uid: String, items: Map<ItemType, Int>): Result<Boolean>
}
