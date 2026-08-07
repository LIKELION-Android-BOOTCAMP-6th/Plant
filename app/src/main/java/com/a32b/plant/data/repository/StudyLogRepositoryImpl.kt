package com.a32b.plant.data.repository

import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.datasource.StudyLogRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyLogRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyLogRepositoryImpl @Inject constructor(
    private val remoteDataSource: StudyLogRemoteDataSource
) : StudyLogRepository {

    override fun getStudyLogs(uid: String, potId: String): Flow<Result<List<StudyLog>>> =
        remoteDataSource.getStudyLogs(uid, potId)
            .map { dtos ->
                val domainModels = dtos.map { it.toDomain() }
                Result.Success(domainModels) as Result<List<StudyLog>>
            }
            .catch { throwable ->
                emit(Result.Failure(mapToAppError(throwable)))
            }

    override suspend fun getPotLogs(uid: String, potId: String): Result<List<StudyLog>> =
        safeRunCatching {
            remoteDataSource.getPotLogs(uid, potId).map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )

    override suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): Result<StudyLog?> =
        safeRunCatching {
            remoteDataSource.getSelectedStudyLog(uid, potId, logId)?.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )

    override suspend fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long): Result<Unit> =
        safeRunCatching {
            val decreaseAmount = studyingTime * -1
            remoteDataSource.executeDeleteBatch(uid, potId, logId, decreaseAmount)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )

    private fun mapToAppError(throwable: Throwable): AppError {
        if (throwable is AppError) return throwable

        return when (throwable) {
            is FirebaseNetworkException -> AppError.Network()
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
                    FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network()
                    else -> AppError.Server(throwable.localizedMessage ?: "서버에 오류가 발생했습니다.")
                }
            }
            else -> AppError.Unknown(throwable.localizedMessage ?: "알 수 없는 오류가 발생했습니다.")
        }
    }
}