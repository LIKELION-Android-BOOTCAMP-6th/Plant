package com.a32b.plant.data.source.remote.studying

import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.model.StudyLogWithPotDto
import com.a32b.plant.data.model.StudySaveStateDto
import com.a32b.plant.data.model.StudyingUserDto
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.Flow

interface StudyingRemoteDataSource {

    /** 태그 기반 학습 중인 유저 구독*/
    fun observeStudyingUser(tag: String) : Flow<List<StudyingUserDto>>

    /** 유저 최초 학습 상태 */
    suspend fun initStudyingUser(user: StudyingUserDto)

    /** 학습 시간 업데이트 */
    suspend fun updateStudyingTime(tag: String, time: Long)

    /**
     * 학습 기록 저장 batch 시작 — 기다리지 않고 Task를 그대로 돌려준다 (기다리는 시간은 Repository가 정함)
     * logs/{sessionId} 생성 + 화분·유저 학습 시간 누적 (firestore.rules의 학습 기록 저장 검증과 짝)
     */
    fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLogDto): Task<Void>

    /** 저장 거부 사유 판단용 서버 상태 (기록·화분·유저를 서버에서 읽음) */
    suspend fun getStudySaveState(uid: String, potId: String, sessionId: String): StudySaveStateDto

    /** 서버에 학습 기록이 있는지 (캐시 아님) */
    suspend fun existsStudyLogOnServer(uid: String, potId: String, sessionId: String): Boolean

    /** 학습 기록 하나 구독 — 문서가 없으면 null */
    fun observeStudyLog(uid: String, potId: String, sessionId: String): Flow<StudyLogDto?>

    /** 지급됐지만 확인하지 않은 보상이 있는 기록 구독 (collectionGroup) */
    fun observeUnseenRewardLogs(uid: String): Flow<List<StudyLogWithPotDto>>

    /** 기록들([potId] to [logId])의 rewardSeen = true — 기다리지 않고 Task를 그대로 돌려준다 */
    fun markRewardsSeen(uid: String, logs: List<Pair<String, String>>): Task<Void>

    /** 학습 종료 후 유저 정보 삭제*/
    suspend fun deleteStudyingUserInfo()
}
