package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.type.PotSortType
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface PotRepository {
    fun getPots(uid: String): Flow<List<Pot>>
    fun getAvailableTags(): Flow<List<Tag>>
    /**
     * 화분 생성 (#640). 오프라인이면 기기 캐시에 바로 반영되고 [SaveResult.Pending]을 반환한다
     * (연결되면 전송됨, 재전송 금지)
     */
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<SaveResult>
    suspend fun getUserPotById(uid: String, potId: String): Pot?
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

    /**
     * 내 화분 정렬 기준 읽기 (#803). 다크모드처럼 기기 설정 저장소에만 둔다 (서버로 보내지 않음, 계정 구분 없음).
     * 저장된 값이 없거나 지금 [PotSortType]에 없는 이름이면 [PotSortType.DEFAULT]
     */
    suspend fun readSortType(): Result<PotSortType>

    /** 내 화분 정렬 기준 저장 (#803). 호출한 화면이 바로 닫혀도 쓰기는 끝까지 진행한다 */
    suspend fun saveSortType(sortType: PotSortType): Result<Unit>

}