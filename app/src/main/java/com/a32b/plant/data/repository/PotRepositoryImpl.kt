package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.firestore.WriteResult
import com.a32b.plant.data.firestore.awaitServerWrite
import com.a32b.plant.data.firestore.isRuleRejection
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.source.remote.pot.InsufficientItemsException
import com.a32b.plant.data.source.remote.pot.PotLevelMismatchException
import com.a32b.plant.data.source.remote.pot.PotNotFoundException
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
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
import kotlin.time.Duration.Companion.seconds

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

    //특정 화분 정보 조회
    override suspend fun getUserPotById(uid: String, potId: String): Pot? {
        val document = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .get().await()
        return document?.toObject(com.a32b.plant.data.model.PotDto::class.java)?.toDomain()
    }

    //중복 제거된 레벨 리스트 조회
    override suspend fun getDuplicationLevelList(uid: String): List<Int> {
        val result = db.collection("users").document(uid).collection("pots")
            .get().await()
        return result.documents
            .mapNotNull { it.getLong("potLevel")?.toInt() }
            .distinct()
            .sorted()
    }

    override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots")
            .whereEqualTo("isCompleted", isCompleted)
            .get()
            .await()
        return snapshot.toObjects(com.a32b.plant.data.model.PotDto::class.java).map { it.toDomain() }
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

    override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult> =
        safeRunCatching {
            // 쓰기를 시작하고(대기열 등록) 서버 반영을 제한 시간만큼만 기다린다.
            // 오프라인이면 응답이 오지 않으므로 시간 초과(Pending)는 실패가 아니라 "전송 대기"로 처리한다.
            // 기기 캐시에는 이미 반영됐고 연결되면 전송되므로, 같은 쓰기를 다시 보내지 않는다.
            potRemoteDataSource.updatePotName(uid, potId, newName)
                .awaitServerWrite(WRITE_TIMEOUT)
        }.fold(
            onSuccess = { write ->
                when (write) {
                    WriteResult.Confirmed -> Result.Success(SaveResult.Saved)
                    WriteResult.Pending -> Result.Success(SaveResult.Pending)
                    is WriteResult.Rejected -> {
                        Log.e("PotRepository", "화분 이름 변경 거부", write.error)
                        Result.Failure(handleError(write.error, RENAME_ERROR_MESSAGE, isWrite = true))
                    }
                }
            },
            onFailure = { e ->
                Log.e("PotRepository", "화분 이름 변경 실패", e)
                Result.Failure(handleError(e, RENAME_ERROR_MESSAGE, isWrite = true))
            }
        )

    override suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult> =
        safeRunCatching {
            // 차감량은 화면 값이 아니라 저장된 화분 값으로 정한다 (규칙이 저장된 값과 같은지 확인)
            val pot = potRemoteDataSource.getPotPreferringCache(uid, potId)
                // 이미 지워진 화분 (연타, 다른 기기에서 삭제) → 보낼 것이 없다
                ?: return@safeRunCatching WriteResult.Confirmed
            potRemoteDataSource.deletePot(uid, potId, pot.potTotalStudyingTime, pot.isCompleted)
                .awaitServerWrite(WRITE_TIMEOUT)
        }.fold(
            onSuccess = { write ->
                write.toSaveResult("화분 삭제", DELETE_POT_ERROR_MESSAGE) {
                    // 거부됐지만 서버에 화분이 없으면 먼저 도착한 같은 삭제가 반영된 것
                    potRemoteDataSource.getPotOnServer(uid, potId) == null
                }
            },
            onFailure = { e ->
                Log.e("PotRepository", "화분 삭제 실패", e)
                Result.Failure(handleError(e, DELETE_POT_ERROR_MESSAGE, isWrite = true))
            }
        )

    override suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult> =
        safeRunCatching {
            val pot = potRemoteDataSource.getPotPreferringCache(uid, potId) ?: throw PotNotFoundException()
            // 이미 완료된 화분은 보내지 않는다 (연타, 다른 기기에서 완료) — 보내더라도 규칙이 거부해 완료 수는 1번만 오른다
            if (pot.isCompleted) return@safeRunCatching WriteResult.Confirmed
            potRemoteDataSource.completePot(uid, potId).awaitServerWrite(WRITE_TIMEOUT)
        }.fold(
            onSuccess = { write ->
                write.toSaveResult("화분 완료", COMPLETE_ERROR_MESSAGE) {
                    // 거부됐지만 서버에서 이미 완료 상태면 먼저 도착한 같은 완료가 반영된 것
                    val serverPot = potRemoteDataSource.getPotOnServer(uid, potId) ?: throw PotNotFoundException()
                    serverPot.isCompleted
                }
            },
            onFailure = { e ->
                Log.e("PotRepository", "화분 완료 실패", e)
                Result.Failure(handleError(e, COMPLETE_ERROR_MESSAGE, isWrite = true))
            }
        )

    /**
     * 서버 반영 결과를 [SaveResult]로 바꾼다 (#455).
     * - 제한 시간 안에 응답이 없으면 [SaveResult.Pending] (실패 아님, 다시 보내지 않음)
     * - 규칙 거부면 규칙은 사유를 알려주지 않으므로 [isAlreadyApplied]로 서버 상태를 확인한다.
     *   이미 반영된 상태면 연타·재전송으로 먼저 도착한 같은 쓰기가 있었던 것이므로 성공으로 본다
     * - 서버 상태를 읽지 못하면(연결 끊김 등) 판단할 수 없으므로 실패
     */
    private suspend fun WriteResult.toSaveResult(
        action: String,
        errorMessage: String,
        isAlreadyApplied: suspend () -> Boolean
    ): Result<SaveResult> = when (this) {
        WriteResult.Confirmed -> Result.Success(SaveResult.Saved)
        WriteResult.Pending -> Result.Success(SaveResult.Pending)
        is WriteResult.Rejected -> {
            Log.e("PotRepository", "$action 거부", error)
            if (!error.isRuleRejection()) {
                Result.Failure(handleError(error, errorMessage, isWrite = true))
            } else {
                safeRunCatching { isAlreadyApplied() }.fold(
                    onSuccess = { applied ->
                        if (applied) Result.Success(SaveResult.Saved)
                        else Result.Failure(AppError.Custom(errorMessage))
                    },
                    onFailure = { e ->
                        Log.e("PotRepository", "$action 거부 후 서버 상태 확인 실패", e)
                        Result.Failure(handleError(e, errorMessage, isWrite = true))
                    }
                )
            }
        }
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

        is InsufficientItemsException -> AppError.InsufficientItems()
        is PotLevelMismatchException -> AppError.Custom("이미 레벨업이 완료된 화분입니다.")
        is PotNotFoundException -> AppError.Custom(POT_NOT_FOUND_MESSAGE)

        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()

            // 없는 문서를 update하면 NOT_FOUND (예: 다른 기기에서 화분을 삭제한 뒤 이름 변경) (#563)
            FirebaseFirestoreException.Code.NOT_FOUND -> AppError.Custom(POT_NOT_FOUND_MESSAGE)

            else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
        }
        is IllegalArgumentException -> AppError.Unknown()
        else -> if (isWrite) AppError.Upload() else AppError.Custom(defaultMessage)
    }

    //레벨업 및 아이템 차감
    override suspend fun levelUpWithItemConsumption(
        uid: String,
        potId: String,
        newLevel: Int,
        items: Map<ItemType, Int>
    ): Result<Unit> {
        return safeRunCatching {
            potRemoteDataSource.levelUpWithItemConsumption(uid, potId, newLevel, items)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                Log.e("PotRepository", "화분 레벨업 및 아이템 차감 실패", e)
                Result.Failure(handleError(e, "화분 레벨업에 실패했습니다.", isWrite = true))
            }
        )
    }

    companion object {
        /** 화분 이름 변경·완료·삭제 제한 시간 — 학습 기록 저장(StudyingRepositoryImpl.SAVE_TIMEOUT)과 같은 5초 */
        private val WRITE_TIMEOUT = 5.seconds
        private const val RENAME_ERROR_MESSAGE = "화분 이름 변경에 실패했습니다."
        private const val COMPLETE_ERROR_MESSAGE = "학습 완료에 실패했습니다. 다시 시도해주세요."
        private const val DELETE_POT_ERROR_MESSAGE = "화분 삭제에 실패했습니다. 다시 시도해주세요."
        private const val POT_NOT_FOUND_MESSAGE = "존재하지 않거나 삭제된 화분입니다."
    }
}
