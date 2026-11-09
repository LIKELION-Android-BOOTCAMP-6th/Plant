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

    /**
     * 화분 삭제 (#455). 유저 학습 시간 차감량·완료 수 감소 여부는 저장된 화분 값으로 정한다.
     * 이미 없는 화분이면(연타·다른 기기에서 삭제) 성공. 하위 학습 기록은 서버(onPotDeleted)가 지운다
     */
    suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult>

    /** 화분 완료 (#455). 이미 완료된 화분이면 보내지 않고 성공, 없는 화분이면 실패 */
    suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult>
    suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>>
    suspend fun levelUpWithItemConsumption(uid: String, potId: String, newLevel: Int, items: Map<ItemType, Int>): Result<Unit>

}