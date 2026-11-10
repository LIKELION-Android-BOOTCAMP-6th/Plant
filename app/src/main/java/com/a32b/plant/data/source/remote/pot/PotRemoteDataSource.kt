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

    /** 기기 캐시의 화분 (캐시에 없으면 기본 조회). 없는 화분이면 null (#455) */
    suspend fun getPotPreferringCache(uid: String, potId: String): PotDto?

    /** 서버의 화분. 없는 화분이면 null — 쓰기가 거부된 뒤 이미 처리됐는지 확인할 때 사용 (#455) */
    suspend fun getPotOnServer(uid: String, potId: String): PotDto?

    /**
     * 화분 완료 batch를 시작하고 서버 반영 Task를 돌려준다 (#455, firestore.rules potCompleted·userPotCompleted).
     * 완료 상태·완료 시각 + 유저 완료 수 +1
     */
    fun completePot(uid: String, potId: String): Task<Void>

    /**
     * 화분 삭제 batch를 시작하고 서버 반영 Task를 돌려준다 (#455, firestore.rules userPotDeleted).
     * 화분 삭제 + 유저 학습 시간 [potTotalStudyingTime]만큼 차감 (+ [isCompleted]면 완료 수 -1).
     * 두 값은 저장된 화분 문서의 값이어야 규칙을 통과한다. 하위 logs는 onPotDeleted 트리거가 지운다
     */
    fun deletePot(uid: String, potId: String, potTotalStudyingTime: Long, isCompleted: Boolean): Task<Void>
}
