package com.a32b.plant.fake

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/** [FakePotRepository.levelUpPot] 호출 기록 */
data class LevelUpRequest(val uid: String, val potId: String, val currentLevel: Int, val items: Map<ItemType, Int>)

/** 테스트 공용 [PotRepository]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다. */
class FakePotRepository(
    var cachedPot: Pot? = null,
    var levelUpResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
    var updatePotNameResult: Result<SaveResult> = Result.Success(SaveResult.Saved)
) : PotRepository {
    val levelUps = mutableListOf<LevelUpRequest>()

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
        return levelUpResult
    }

    override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult> {
        updates += Triple(uid, potId, newName)
        return updatePotNameResult
    }

    override fun getPots(uid: String): Flow<List<Pot>> = error("Unused")
    override fun getAvailableTags(): Flow<List<Tag>> = error("Unused")
    override suspend fun addPot(uid: String, tag: Tag, name: String): Result<SaveResult> = error("Unused")
    override suspend fun getUserPotById(uid: String, potId: String): Pot? = error("Unused")
    override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> = error("Unused")
    override suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult> = error("Unused")
    override suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult> = error("Unused")
    override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> =
        error("Unused")
}
