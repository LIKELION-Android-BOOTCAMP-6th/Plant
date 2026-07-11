package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface PotRepository {
    fun getPots(uid: String): Flow<List<Pot>>
    fun getAvailableTags(): Flow<List<Tag>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun updatePotLevel(uid: String, potId: String, newLevel: String): Result<Unit>
    suspend fun getUserPotById(uid: String, potId: String): Pot?
    suspend fun getPotLogs(uid: String, potId: String): List<StudyLog>
    suspend fun getDuplicationLevelList(uid: String): List<String>
    fun createStudyLog(potId: String, studyLog: StudyLog)
    fun updateTotalStudyTime(potId: String, studyTime: Long)
    suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot>
    suspend fun getSelectedStudyLog(potId: String, logId: String): StudyLog?
}