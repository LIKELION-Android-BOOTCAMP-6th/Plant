package com.a32b.plant.domain.repository

import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface StudyingRepository {

    /** 같은 태그로 학습 중인 사용자 구독*/
    fun observeStudyingUser(tag: String): Flow<List<StudyingUser>>

    /** 공부 시작 시 유저 정보 저장 */
    suspend fun initStudyingUser(user: StudyingUser): Result<Unit>

    /** 학습 시간 업데이트 */
    suspend fun updateStudyingTime(tag: String, time: Long): Result<Unit>

    /** 학습 종료 시 화분 총 공부 시간 업데이트 */
    suspend fun updateTotalStudyTime(potId: String, studyTime: Long): Result<Unit>

    /** 유저 총 학습 시간 업데이트 */
    suspend fun updateUserTotalStudyTime(time: Long) : Result<Unit>

    /** 학습 기록 저장 */
    suspend fun saveStudyLog(potId: String, log: StudyLog): Result<Unit>

    /** 학습 종료 시 유저 정보 삭제 */
    suspend fun deleteStudyingUserInfo(): Result<Unit>

    /** 특정 화분의 학습기록 목록 조회*/
    suspend fun getStudyLogs(potId: String): Result<List<StudyLog>>

    /** 로컬 DB 저장/읽기/삭제 */
    suspend fun saveLocalSession(studying : StudyingSession): Result<Unit>

    suspend fun readLocalSession(): Result<StudyingSession?>

    suspend fun clearLocalSession(): Result<Unit>

}
