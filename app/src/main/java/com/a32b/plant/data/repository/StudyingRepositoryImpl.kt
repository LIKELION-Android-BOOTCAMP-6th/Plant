package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSource
import com.a32b.plant.data.local.StudyingLocalDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class StudyingRepositoryImpl @Inject constructor(
    private val studyingRemoteDataSource: StudyingRemoteDataSource,
    private val local: StudyingLocalDataSource
): StudyingRepository {
    override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> {
        return studyingRemoteDataSource.observeStudyingUser(tag)
            .map { dtoList -> dtoList.map { it.toDomain() } }
    }

    override suspend fun initStudyingUser(user: StudyingUser): Result<Unit> = runCatching {
        studyingRemoteDataSource.initStudyingUser(user.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            if (e is CancellationException) throw e

            Log.e("Studying", "스터딩 유저 초기 저장 실패", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.Unknown("유저 정보를 찾을 수 없습니다.")
                else -> AppError.Update()
            }
            Result.Failure(error)
        }
    )

    override suspend fun updateStudyingTime(tag: String, time: Long): Result<Unit> = runCatching {
        studyingRemoteDataSource.updateStudyingTime(tag, time)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            if (e is CancellationException) throw e

            Log.e("Studying", "학습 시간 업데이트", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.Unknown("유저 정보를 찾을 수 없습니다.")
                else -> AppError.Update()
            }

            Result.Failure(error)
        }
    )

    override suspend fun updateTotalStudyTime(potId: String, studyTime: Long) = runCatching {
        studyingRemoteDataSource.updateTotalStudyTime(potId, studyTime)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "화분 총 학습 시간 업데이트", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.Unknown("유저 정보를 찾을 수 없습니다.")
                else -> AppError.Update()
            }
            Result.Failure(error)
        }
    )

    override suspend fun saveStudyLog(potId: String, log: StudyLog): Result<Unit> {
        val result = runCatching { studyingRemoteDataSource.saveStudyLog(potId, log.toDto()) }
            .recoverCatching {
                if (it is CancellationException) throw it
                studyingRemoteDataSource.saveStudyLog(potId, log.toDto())
            }

        return result.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                if (e is CancellationException) throw e
                Log.e("Studying", "학습 기록 저장", e)

                val error = when (e) {
                    is IllegalArgumentException -> AppError.Unknown("유저 정보를 찾을 수 없습니다.")
                    else -> AppError.Custom("학습 기록 저장에 실패했습니다.")
                }

                Result.Failure(error)
            }
        )
    }

    override suspend fun deleteStudyingUserInfo(): Result<Unit> = runCatching {
        studyingRemoteDataSource.deleteStudyingUserInfo()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "학습중 유저 정보 삭제", e)
            val error = when(e){
                is IllegalArgumentException -> AppError.Unknown("유저 정보를 찾을 수 없습니다.")
                else -> AppError.Custom("")
            }

            Result.Failure(error)
        }
    )

    override suspend fun saveLocalSession(studying: StudyingSession) = runCatching {
        local.save(studying)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("studying", "학습 세션 로컬 저장", e)
            Result.Failure(AppError.Custom("세션 저장 실패"))
        }
    )

    override suspend fun readLocalSession(): Result<StudyingSession?> = runCatching {
        local.read()
    }.fold(
        onSuccess = { Result.Success(it)},
        onFailure = { e ->
            Log.e("studying", "학습중 세션 읽기", e)
            Result.Failure(AppError.Custom("불러오기 실패"))
        }
    )

    override suspend fun clearLocalSession() = runCatching {
        local.clear()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = {e ->
            Log.e("studying", "세션 초기화", e)
            Result.Failure(AppError.Custom("세션 초기화 실패"))
        }
    )

}