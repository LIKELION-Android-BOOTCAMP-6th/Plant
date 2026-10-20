package com.a32b.plant.data.source.remote.pot

import com.a32b.plant.data.model.PotDto
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.Tag
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PotRemoteDataSource {
    fun getPots(uid: String): Flow<List<PotDto>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun updatePotLevel(uid: String, potId: String, newLevel: Int): Result<Unit>
    suspend fun getPotsOnce(uid: String): List<PotDto>
    suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): List<StudyLogDto>
}
