package com.a32b.plant.fake

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.type.PotSortType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import java.util.Date

/** [FakePotRepository.levelUpPot] 호출 기록 */
data class LevelUpRequest(val uid: String, val potId: String, val currentLevel: Int, val items: Map<ItemType, Int>)

/** 테스트 공용 [PotRepository]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다. */
class FakePotRepository(
    var cachedPot: Pot? = null,
    var levelUpResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
    var updatePotNameResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
    /** getPots가 돌려줄 Flow — 넣지 않으면 호출 시 실패 */
    var pots: Flow<List<Pot>>? = null,
    /** getTodayStudyLogs 결과 — 넣지 않으면 호출 시 실패 */
    var todayStudyLogsResult: Result<List<StudyLog>>? = null,
    /** getAvailableTags가 돌려줄 Flow — 넣지 않으면 호출 시 실패 */
    var tagsFlow: Flow<List<Tag>>? = null,
    /** addPot 결과 — 넣지 않으면 호출 시 실패 */
    var addPotResult: Result<SaveResult>? = null
) : PotRepository {
    /** addPot 호출 기록 (uid, tag, name) */
    val addedPots = mutableListOf<Triple<String, Tag, String>>()

    /** 넣으면 addPot이 게이트가 완료될 때까지 끝나지 않는다 (저장 대기 재현) */
    var addPotGate: CompletableDeferred<Unit>? = null

    val levelUps = mutableListOf<LevelUpRequest>()

    /** 넣으면 levelUpPot이 게이트가 완료될 때까지 끝나지 않는다 (처리 중 상태 확인용) */
    var levelUpGate: CompletableDeferred<Unit>? = null

    /** 넣으면 levelUpPot이 결과를 돌려주기 전에 호출된다 (쓰기가 기기 캐시에 먼저 반영되는 상황 흉내용) */
    var onLevelUp: (() -> Unit)? = null

    /** updatePotName 호출 기록 (uid, potId, newName) */
    val updates = mutableListOf<Triple<String, String, String>>()

    override suspend fun getPotPreferringCache(uid: String, potId: String): Result<Pot?> = Result.Success(cachedPot)

    override suspend fun levelUpPot(
        uid: String,
        potId: String,
        currentLevel: Int,
        items: Map<ItemType, Int>
    ): Result<SaveResult> {
        levelUps += LevelUpRequest(uid, potId, currentLevel, items)
        onLevelUp?.invoke()
        levelUpGate?.await()
        return levelUpResult
    }

    override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult> {
        updates += Triple(uid, potId, newName)
        return updatePotNameResult
    }

    override fun getPots(uid: String): Flow<List<Pot>> = pots ?: error("Unused")
    override fun getAvailableTags(): Flow<List<Tag>> = tagsFlow ?: error("Unused")
    override suspend fun addPot(uid: String, tag: Tag, name: String): Result<SaveResult> {
        val result = addPotResult ?: error("Unused")
        addedPots += Triple(uid, tag, name)
        addPotGate?.await()
        return result
    }
    override suspend fun getUserPotById(uid: String, potId: String): Pot? = error("Unused")
    override suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult> = error("Unused")
    override suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult> = error("Unused")
    override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> =
        todayStudyLogsResult ?: error("Unused")

    /** readSortType 결과 — 넣지 않으면 호출 시 실패 */
    var readSortTypeResult: Result<PotSortType>? = null

    /** 넣으면 readSortType이 게이트가 완료될 때까지 끝나지 않는다 (저장된 정렬 읽기 전 상태 확인용) */
    var readSortTypeGate: CompletableDeferred<Unit>? = null

    var saveSortTypeResult: Result<Unit> = Result.Success(Unit)

    /** saveSortType 호출 기록 */
    val savedSortTypes = mutableListOf<PotSortType>()

    override suspend fun readSortType(): Result<PotSortType> {
        val result = readSortTypeResult ?: error("Unused")
        readSortTypeGate?.await()
        return result
    }

    override suspend fun saveSortType(sortType: PotSortType): Result<Unit> {
        savedSortTypes += sortType
        return saveSortTypeResult
    }
}
