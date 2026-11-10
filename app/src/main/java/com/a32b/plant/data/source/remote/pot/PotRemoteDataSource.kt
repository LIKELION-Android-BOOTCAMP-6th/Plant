package com.a32b.plant.data.source.remote.pot

import com.a32b.plant.data.model.PotDto
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.type.ItemType
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PotRemoteDataSource {
    fun getPots(uid: String): Flow<List<PotDto>>
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit>
    suspend fun getPotsOnce(uid: String): GetPotsOnceResult
    suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): List<StudyLogDto>
    suspend fun levelUpWithItemConsumption(uid: String, potId: String, newLevel: Int, items: Map<ItemType, Int>)

    /** 화분 이름 변경 쓰기를 시작하고, 서버 반영을 나타내는 Task를 돌려준다. (오프라인이면 Task가 끝나지 않음) */
    fun updatePotName(uid: String, potId: String, name: String): Task<Void>
}
