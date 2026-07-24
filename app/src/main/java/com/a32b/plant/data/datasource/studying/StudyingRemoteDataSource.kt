package com.a32b.plant.data.datasource.studying

import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.model.StudyingUserDto
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface StudyingRemoteDataSource {

    /** 태그 기반 학습 중인 유저 구독*/
    fun observeStudyingUser(tag: String) : Flow<List<StudyingUserDto>>

    /** 유저 최초 학습 상태 */
    suspend fun initStudyingUser(user: StudyingUserDto)

    /** 학습 시간 업데이트 */
    suspend fun updateStudyingTime(tag: String, time: Long)

    /** 학습 기록 저장*/
    suspend fun saveStudyLog(potId: String, log: StudyLogDto)

    /** 학습 종료 후 유저 정보 삭제*/
    suspend fun deleteStudyingUserInfo()

    /** 학습 종료 후 화분 총 시간 업데이트*/
    suspend fun updateTotalStudyTime(potId: String, time: Long)

    /** 학습 종료 후 유저 총 공부 시간 업데이트*/
    suspend fun updateUserTotalStudyTime(time: Long)
}
