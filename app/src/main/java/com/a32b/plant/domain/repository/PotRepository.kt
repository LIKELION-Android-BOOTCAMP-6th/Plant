package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface PotRepository {
    fun getPots(uid: String): Flow<List<Pot>>
    fun getAvailableTags(): Flow<List<Tag>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun updatePotLevel(uid: String, potId: String, newLevel: String): Result<Unit>
    suspend fun getUserPotById(uid: String, potId: String): Pot?
    suspend fun getDuplicationLevelList(uid: String): List<String>
    suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot>
    suspend fun updatePotName(uid: String, potId: String, newName: String)
    suspend fun deleteEntirePot(uid: String, potId: String, totalStudyingTime: Long)
    suspend fun completeStudyPlan(uid: String, potId: String)
}