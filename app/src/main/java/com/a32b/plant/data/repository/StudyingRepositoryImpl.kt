package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.remote.studying.StudyingRemoteDataSource
import com.a32b.plant.data.source.local.StudyingLocalDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.model.StudyingSession
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class StudyingRepositoryImpl @Inject constructor(
    private val studyingRemoteDataSource: StudyingRemoteDataSource,
    private val local: StudyingLocalDataSource,
    private val db : FirebaseFirestore,
    private val userRepository: UserRepository
): StudyingRepository {
    override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> {
        return studyingRemoteDataSource.observeStudyingUser(tag)
            .map { dtoList -> dtoList.map { it.toDomain() } }
    }

    override suspend fun initStudyingUser(user: StudyingUser): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.initStudyingUser(user.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "스터딩 유저 초기 저장 실패", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.UnknownUser()
                else -> AppError.Upload()
            }
            Result.Failure(error)
        }
    )

    override suspend fun updateStudyingTime(tag: String, time: Long): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.updateStudyingTime(tag, time)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "학습 시간 업데이트", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.UnknownUser()
                else -> AppError.Upload()
            }

            Result.Failure(error)
        }
    )

    override suspend fun updateTotalStudyTime(potId: String, studyTime: Long) = safeRunCatching {
        studyingRemoteDataSource.updateTotalStudyTime(potId, studyTime)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "화분 총 학습 시간 업데이트", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.UnknownUser()
                else -> AppError.Upload()
            }
            Result.Failure(error)
        }
    )

    override suspend fun updateUserTotalStudyTime(time: Long): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.updateUserTotalStudyTime(time)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = {e ->
            Log.e("Studying", "유저 총 학습 시간 업데이트", e)

            val error = when(e){
                is IllegalArgumentException -> AppError.UnknownUser()
                else -> AppError.Upload()
            }
            Result.Failure(error)
        }
    )

    override suspend fun saveStudyLog(potId: String, log: StudyLog): Result<Unit> = safeRunCatching {
        val uid = userRepository.currentUser.value?.uid ?: return Result.Failure(AppError.UnknownUser())
        val logId = db.collection("users")
            .document(uid)
            .collection("pots")
            .document(potId)
            .collection("logs")
            .document().id
        studyingRemoteDataSource.saveStudyLog(uid,potId, logId,log.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("Studying", "학습 기록 저장", e)

            val error = when (e) {
                is FirebaseFirestoreException -> when (e.code) {
                    FirebaseFirestoreException.Code.UNAVAILABLE,
                    FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Log.e("Studying", "⚠️ Firestore 규칙 위반 - 규칙 또는 요청 데이터 확인 필요", e)
                        AppError.Permission()
                    }
                    else -> AppError.Upload()
                }
                else -> AppError.Upload()
            }
            Result.Failure(error)
        }
    )
    override suspend fun deleteStudyingUserInfo(): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.deleteStudyingUserInfo()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "학습중 유저 정보 삭제", e)
            val error = when(e){
                is IllegalArgumentException -> AppError.UnknownUser()
                else -> AppError.Custom("")
            }

            Result.Failure(error)
        }
    )

    override suspend fun saveLocalSession(studying: StudyingSession) = safeRunCatching {
        local.save(studying)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("studying", "학습 세션 로컬 저장", e)
            val error = when(e){
                is IOException -> AppError.Custom("저장 공간이 부족하여 비정상 종료 시 데이터를 소실할 수 있습니다.")
                else -> AppError.Local()
            }
            Result.Failure(error)
        }
    )

    override suspend fun readLocalSession(): Result<StudyingSession?> = safeRunCatching {
        local.read()
    }.fold(
        onSuccess = { Result.Success(it)},
        onFailure = { e ->
            Log.e("studying", "학습중 세션 읽기", e)
            Result.Failure(AppError.Local("불러오기 실패"))
        }
    )

    override suspend fun clearLocalSession() = runCatching {
        local.clear()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = {e ->
            Log.e("studying", "세션 초기화", e)
            Result.Failure(AppError.Local("세션 초기화 실패"))
        }
    )

}
