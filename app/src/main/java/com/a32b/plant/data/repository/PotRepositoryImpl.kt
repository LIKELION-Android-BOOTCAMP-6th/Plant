package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotRepositoryImpl @Inject constructor(
    private val potRemoteDataSource: PotRemoteDataSource,
    private val db: FirebaseFirestore
) : PotRepository {

    override fun getPots(uid: String): Flow<List<Pot>> =
        potRemoteDataSource.getPots(uid)
            .map { dtoList -> dtoList.map { it.toDomain() } }
            // 로그아웃 등으로 인증 토큰이 무효화된 직후에도 리스너가 잠깐 살아있을 수 있다.
            // 이때 서버가 PERMISSION_DENIED를 반환하는데, 처리하지 않으면 앱 전체가 죽는다.
            .catch { e -> Log.e("PotRepository", "화분 목록 구독 실패: ${e.message}", e) }

    override suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> {
        return safeRunCatching {
            potRemoteDataSource.addPot(uid, tag, name).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "화분 추가 실패", e)
                Result.Failure(handleError(e, "화분 추가에 실패했습니다.", isWrite = true))
            }
        )
    }

    override suspend fun updatePotLevel(uid: String, potId: String, newLevel: String): Result<Unit> {
        return safeRunCatching {
            potRemoteDataSource.updatePotLevel(uid, potId, newLevel).getOrThrow()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "화분 레벨 업데이트 실패", e)
                Result.Failure(handleError(e, "화분 레벨 업데이트에 실패했습니다.", isWrite = true))
            }
        )
    }

    //특정 화분 정보 조회
    override suspend fun getUserPotById(uid: String, potId: String): Result<Pot?> {
        return safeRunCatching {
            val document = db.collection("users").document(uid)
                .collection("pots").document(potId)
                .get().await()
            document?.toObject(com.a32b.plant.data.model.PotDto::class.java)?.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "특정 화분 조회 실패", e)
                Result.Failure(handleError(e, "화분 정보를 불러오는데 실패했습니다."))
            }
        )
    }

    //중복 제거된 레벨 리스트 조회
    override suspend fun getDuplicationLevelList(uid: String): Result<List<String>> {
        return safeRunCatching {
            val result = db.collection("users").document(uid).collection("pots")
                .get().await()
            result.documents
                .mapNotNull { it.getString("level") }
                .distinct()
                .sorted()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "레벨 리스트 조회 실패", e)
                Result.Failure(handleError(e, "레벨 목록을 불러오는데 실패했습니다."))
            }
        )
    }

    override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): Result<List<Pot>> {
        return safeRunCatching {
            val snapshot = db.collection("users").document(uid)
                .collection("pots")
                .whereEqualTo("isCompleted", isCompleted)
                .get()
                .await()
            snapshot.toObjects(com.a32b.plant.data.model.PotDto::class.java).map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "상태별 화분 목록 조회 실패", e)
                Result.Failure(handleError(e, "화분 목록을 불러오는데 실패했습니다."))
            }
        )
    }

    override fun getAvailableTags(): Flow<List<Tag>> = callbackFlow {
        val collectionRef = db.collection("Tags").orderBy("no")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val tags = snapshot?.toObjects(Tag::class.java) ?: emptyList()
            trySend(tags)
        }
        awaitClose { listener.remove() }
    }.catch { e -> Log.e("PotRepository", "태그 목록 구독 실패: ${e.message}", e) }

    override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<Unit> {
        return safeRunCatching {
            db.collection("users").document(uid)
                .collection("pots").document(potId)
                .update("name", newName)
                .await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Log.e("PotRepository", "화분 이름 수정 실패", e)
                Result.Failure(handleError(e, "화분 이름 변경에 실패했습니다.", isWrite = true))
            }
        )
    }

    override suspend fun deleteEntirePot(uid: String, potId: String, totalStudyingTime: Long): Result<Unit> {
        return safeRunCatching {
            val userRef = db.collection("users").document(uid)
            val potRef = userRef.collection("pots").document(potId)

            db.runBatch { batch ->
                batch.update(userRef, "totalStudyTime", FieldValue.increment(totalStudyingTime * -1))
                batch.delete(potRef)
            }.await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Log.e("PotRepository", "화분 삭제 실패", e)
                Result.Failure(handleError(e, "화분 삭제에 실패했습니다.", isWrite = true))
            }
        )
    }

    override suspend fun completeStudyPlan(uid: String, potId: String): Result<Unit> {
        return safeRunCatching {
            val userRef = db.collection("users").document(uid)
            val potRef = userRef.collection("pots").document(potId)

            db.runBatch { batch ->
                batch.update(userRef, "completedPotsCount", FieldValue.increment(1))
                batch.update(
                    potRef, mapOf(
                        "isCompleted" to true,
                        "completedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e ->
                Log.e("PotRepository", "학습 완료 처리 실패", e)
                Result.Failure(handleError(e, "학습 완료 처리에 실패했습니다.", isWrite = true))
            }
        )
    }

    override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> {
        return safeRunCatching {
            val dtoList = potRemoteDataSource.getTodayStudyLogs(uid, potId, startOfToday)
            dtoList.map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { throwable ->
                Log.e("PotRepository", "오늘 학습 로그 조회 실패: ${throwable.message}", throwable)
                Result.Failure(handleError(throwable, "오늘 학습 시간 조회 중 오류가 발생했습니다."))
            }
        )
    }

    private fun handleError(e: Throwable, defaultMessage: String, isWrite: Boolean = false): AppError = when (e) {
        is AppError -> e
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()

            else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
        }
        is IllegalArgumentException -> AppError.Unknown()
        else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
    }
}
