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

    /** 기기 캐시의 화분 (캐시에 없으면 기본 조회). 없는 화분이면 null — 레벨업 조건을 오프라인에서도 확인하기 위함 (#456) */
    suspend fun getPotPreferringCache(uid: String, potId: String): Result<Pot?>

    /**
     * 화분 레벨업 (#456). 레벨 [currentLevel] + 1과 아이템 [items] 차감을 batch 하나로 쓴다.
     * 학습 시간·아이템 조건은 호출부가 먼저 확인하고, 서버에서는 보안 규칙이 같은 조건 표로 다시 검증한다.
     * 거부됐지만 서버 레벨이 이미 목표 레벨이면(연타·재전송) 성공
     */
    suspend fun levelUpPot(uid: String, potId: String, currentLevel: Int, items: Map<ItemType, Int>): Result<SaveResult>

}