package com.a32b.plant.data.datasource.pot

import com.a32b.plant.data.model.PotDto
import com.a32b.plant.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface PotRemoteDataSource {
    fun getPots(uid: String): Flow<List<PotDto>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun updatePotLevel(uid: String, potId: String, newLevel: String): Result<Unit>
}