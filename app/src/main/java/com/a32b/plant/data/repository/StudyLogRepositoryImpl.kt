package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.datasource.StudyLogRemoteDataSource
import com.a32b.plant.data.firestore.WriteResult
import com.a32b.plant.data.firestore.awaitServerWrite
import com.a32b.plant.data.firestore.isRuleRejection
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.SaveResult
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
import kotlin.time.Duration.Companion.seconds

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

    override suspend fun deleteStudyLog(uid: String, potId: String, logId: String): Result<SaveResult> =
        safeRunCatching {
            // 차감량은 화면 값이 아니라 저장된 기록 값으로 정한다 (규칙이 저장된 값과 같은지 확인)
            val log = remoteDataSource.getStudyLogPreferringCache(uid, potId, logId)
                // 이미 지워진 기록 (연타, 다른 기기에서 삭제) → 보낼 것이 없다
                ?: return@safeRunCatching WriteResult.Confirmed
            remoteDataSource.deleteStudyLog(uid, potId, logId, log.studyingTime)
                .awaitServerWrite(DELETE_TIMEOUT)
        }.fold(
            onSuccess = { write ->
                when (write) {
                    WriteResult.Confirmed -> Result.Success(SaveResult.Saved)
                    // 오프라인: 기기 캐시에는 이미 반영됐고 연결되면 전송된다 → 다시 보내지 않는다
                    WriteResult.Pending -> Result.Success(SaveResult.Pending)
                    is WriteResult.Rejected -> resolveRejectedDelete(uid, potId, logId, write.error)
                }
            },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )

    /**
     * 기록 삭제가 거부됐을 때 — 규칙은 사유를 알려주지 않으므로 서버 상태를 읽어 판단한다 (#455)
     * - 서버에 기록이 없으면 먼저 도착한 같은 삭제(연타·재전송)가 반영된 것 → 성공 (차감은 1번만 됨)
     * - 기록이 남아 있으면 실패 (기기 캐시도 되돌아가 기록이 다시 보인다)
     * - 서버 상태를 읽지 못하면(연결 끊김 등) 판단할 수 없으므로 실패
     */
    private suspend fun resolveRejectedDelete(
        uid: String,
        potId: String,
        logId: String,
        error: Throwable
    ): Result<SaveResult> {
        Log.e("StudyLogRepositoryImpl", "학습 기록 삭제 거부", error)
        if (!error.isRuleRejection()) return Result.Failure(mapToAppError(error))

        return safeRunCatching {
            remoteDataSource.existsStudyLogOnServer(uid, potId, logId)
        }.fold(
            onSuccess = { exists ->
                if (exists) Result.Failure(AppError.Custom(DELETE_ERROR_MESSAGE))
                else Result.Success(SaveResult.Saved)
            },
            onFailure = { e -> Result.Failure(mapToAppError(e)) }
        )
    }

    private fun mapToAppError(throwable: Throwable): AppError {
        if (throwable is AppError) return throwable
        Log.e("StudyLogRepositoryImpl", "Original error: ${throwable.stackTraceToString()}")
        return when (throwable) {
            is FirebaseNetworkException -> AppError.Network()
            is FirebaseFirestoreException -> {
                when (throwable.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
                    FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.Network()
                    else -> AppError.Server()
                }
            }
            else -> AppError.Unknown()
        }
    }

    companion object {
        /** 기록 삭제 제한 시간 — 학습 기록 저장(StudyingRepositoryImpl.SAVE_TIMEOUT)과 같은 5초 */
        private val DELETE_TIMEOUT = 5.seconds
        private const val DELETE_ERROR_MESSAGE = "학습 기록 삭제에 실패했습니다. 다시 시도해주세요."
    }
}