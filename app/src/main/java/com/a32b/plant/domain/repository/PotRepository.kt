package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PotRepository {
    fun getPots(uid: String): Flow<List<Pot>>
    fun getAvailableTags(): Flow<List<Tag>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun getUserPotById(uid: String, potId: String): Pot?
    suspend fun getDuplicationLevelList(uid: String): List<Int>
    suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot>
    suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult>
    suspend fun deleteEntirePot(uid: String, potId: String, totalStudyingTime: Long)
    suspend fun completeStudyPlan(uid: String, potId: String)
    suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>>
    suspend fun levelUpWithItemConsumption(uid: String, potId: String, newLevel: Int, items: Map<ItemType, Int>): Result<Unit>

}