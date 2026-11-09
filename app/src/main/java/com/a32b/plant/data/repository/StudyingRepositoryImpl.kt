package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.firestore.WriteResult
import com.a32b.plant.data.firestore.awaitPendingWrites
import com.a32b.plant.data.firestore.awaitServerWrite
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.mapper.toRewardStatus
import com.a32b.plant.data.model.StudySaveStateDto
import com.a32b.plant.data.source.local.StudyingLocalDataSource
import com.a32b.plant.data.source.remote.studying.StudyingRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyLogRef
import com.a32b.plant.domain.model.StudyRewardStatus
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingGoalDraft
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.model.UnseenStudyReward
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.StudySaveRejection
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds


@Singleton
class StudyingRepositoryImpl @Inject constructor(
    private val studyingRemoteDataSource: StudyingRemoteDataSource,
    private val local: StudyingLocalDataSource,
    private val db : FirebaseFirestore
): StudyingRepository {
    override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> {
        return studyingRemoteDataSource.observeStudyingUser(tag)
            .map { dtoList -> dtoList.map { it.toDomain() } }
            .catch { e -> Log.e("StudyingRepository", "학습중 유저 구독 실패: ${e.message}", e) }
    }

    override suspend fun initStudyingUser(user: StudyingUser): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.initStudyingUser(user.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "스터딩 유저 초기 저장 실패", e)

            val error = when(e){
                is AppError.UnknownUser -> AppError.UnknownUser()
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
                is AppError.UnknownUser -> AppError.UnknownUser()
                else -> AppError.Upload()
            }

            Result.Failure(error)
        }
    )

    override suspend fun saveStudySession(
        uid: String,
        potId: String,
        sessionId: String,
        log: StudyLog
    ): Result<StudySaveResult> = safeRunCatching {
        // 쓰기를 시작하고(대기열 등록) 서버 반영을 제한 시간만큼만 기다린다.
        // 오프라인이면 응답이 오지 않으므로 시간 초과(Pending)는 실패가 아니라 "전송 대기"로 처리한다 (#452).
        // 기기 캐시에는 이미 반영됐고 연결되면 전송되므로, 같은 쓰기를 다시 보내지 않는다.
        studyingRemoteDataSource.saveStudySession(uid, potId, sessionId, log.toDto())
            .awaitServerWrite(SAVE_TIMEOUT)
    }.fold(
        onSuccess = { write ->
            when (write) {
                WriteResult.Confirmed -> Result.Success(StudySaveResult.Saved)
                WriteResult.Pending -> Result.Success(StudySaveResult.Pending)
                is WriteResult.Rejected -> resolveRejectedSave(uid, potId, sessionId, log, write.error)
            }
        },
        onFailure = { e ->
            Log.e("Studying", "학습 기록 저장 실패", e)
            Result.Failure(handleSaveError(e))
        }
    )

    /**
     * 서버가 학습 기록 저장을 거부했을 때 — 규칙은 사유를 알려주지 않으므로 서버 상태를 읽어 판단한다
     * - 규칙 거부가 아닌 오류는 다시 보내면 될 수 있으므로 실패로 돌려 백업을 남긴다
     * - 서버 상태를 읽지 못하면(연결 끊김) 판단할 수 없으므로 실패로 돌려 백업을 남긴다 (다음 복구에서 다시 판단)
     * - 사유를 찾지 못한 거부도 실패로 돌려 백업을 남긴다 ([toSaveResult] 참고)
     */
    private suspend fun resolveRejectedSave(
        uid: String,
        potId: String,
        sessionId: String,
        log: StudyLog,
        error: Throwable
    ): Result<StudySaveResult> {
        Log.e("Studying", "학습 기록 저장 거부", error)
        if (!isRuleRejection(error)) return Result.Failure(handleSaveError(error))

        return safeRunCatching {
            studyingRemoteDataSource.getStudySaveState(uid, potId, sessionId)
        }.fold(
            onSuccess = { state -> state.toSaveResult(log) },
            onFailure = { e ->
                Log.e("Studying", "학습 기록 저장 거부 사유 확인 실패", e)
                Result.Failure(handleSaveError(e))
            }
        )
    }

    /** 다시 보내도 결과가 같은 거부 — 규칙 거부(PERMISSION_DENIED), 문서 없음(NOT_FOUND: 없는 화분 update) */
    private fun isRuleRejection(e: Throwable): Boolean =
        e is FirebaseFirestoreException && (
            e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                e.code == FirebaseFirestoreException.Code.NOT_FOUND
            )

    /**
     * 거부된 뒤 서버 상태로 사유 판단 — 순서는 firestore.rules 학습 기록 저장 검증의 확인 항목 기준
     * 다시 보내도 안 되는 거부로 확인된 경우만 [StudySaveResult.Rejected] (호출부가 백업 삭제)
     * 사유를 찾지 못하면 실패로 돌려 백업을 남긴다. 대표적인 경우는 기기 시계가 허용 오차(5분)보다 빨라
     * 종료 시각이 서버 시각보다 늦은 것인데, 이 거부는 시간이 지나면 같은 기록이 통과하므로 지우면 안 된다
     */
    private fun StudySaveStateDto.toSaveResult(log: StudyLog): Result<StudySaveResult> {
        val reason = when {
            // 같은 세션이 먼저 저장됨 (대기열의 저장 + 재전송, 응답 유실 후 복구)
            logExists -> return Result.Success(StudySaveResult.AlreadySaved)
            !potExists -> StudySaveRejection.POT_NOT_FOUND
            potCompleted -> StudySaveRejection.POT_COMPLETED
            log.studyingTime <= 0L || log.studyingTime > StudyLog.MAX_STUDYING_TIME_MS ->
                StudySaveRejection.INVALID_TIME
            (lastStudyEndedAt?.toDate()?.time ?: 0L) > log.startedAt -> StudySaveRejection.OVERLAPPED
            else -> return Result.Failure(AppError.Custom(UNRESOLVED_REJECTION_MESSAGE))
        }
        return Result.Success(StudySaveResult.Rejected(reason))
    }

    private fun handleSaveError(e: Throwable): AppError = when (e) {
        is AppError -> e
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

    override suspend fun awaitPendingWrites(): Boolean = db.awaitPendingWrites(PENDING_WRITES_TIMEOUT)

    override suspend fun isStudySessionSaved(uid: String, potId: String, sessionId: String): Result<Boolean> = safeRunCatching {
        studyingRemoteDataSource.existsStudyLogOnServer(uid, potId, sessionId)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e ->
            Log.e("Studying", "학습 기록 서버 저장 여부 확인 실패", e)
            Result.Failure(handleSaveError(e))
        }
    )

    override fun observeStudyReward(uid: String, potId: String, sessionId: String): Flow<StudyRewardStatus?> =
        studyingRemoteDataSource.observeStudyLog(uid, potId, sessionId)
            .map { dto -> dto?.toRewardStatus() }

    override fun observeUnseenStudyRewards(uid: String): Flow<List<UnseenStudyReward>> =
        studyingRemoteDataSource.observeUnseenRewardLogs(uid)
            .map { logs ->
                logs.mapNotNull { (potId, log) ->
                    val status = log.toRewardStatus() as? StudyRewardStatus.Granted ?: return@mapNotNull null
                    UnseenStudyReward(StudyLogRef(potId = potId, logId = log.id), status.reward)
                }
            }

    override suspend fun markStudyRewardsSeen(uid: String, refs: List<StudyLogRef>): Result<Unit> {
        if (refs.isEmpty()) return Result.Success(Unit)
        return safeRunCatching {
            studyingRemoteDataSource.markRewardsSeen(uid, refs.map { it.potId to it.logId })
                .awaitServerWrite(SAVE_TIMEOUT)
        }.fold(
            onSuccess = { write ->
                when (write) {
                    // 오프라인이면 기기 캐시에 먼저 반영되어 화면에서는 사라지고, 연결되면 전송된다
                    WriteResult.Confirmed, WriteResult.Pending -> Result.Success(Unit)
                    is WriteResult.Rejected -> {
                        Log.e("Studying", "보상 확인 표시 거부", write.error)
                        Result.Failure(handleSaveError(write.error))
                    }
                }
            },
            onFailure = { e ->
                Log.e("Studying", "보상 확인 표시 실패", e)
                Result.Failure(handleSaveError(e))
            }
        )
    }

    override suspend fun deleteStudyingUserInfo(): Result<Unit> = safeRunCatching {
        studyingRemoteDataSource.deleteStudyingUserInfo()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e ->
            Log.e("Studying", "학습중 유저 정보 삭제", e)
            val error = when(e){
                is AppError.UnknownUser -> AppError.UnknownUser()
                else -> AppError.Custom("유저 정보 삭제 실패")
            }

            Result.Failure(error)
        }
    )

    override suspend fun saveLocalSession(studying: StudyingSession) = safeRunCatching {
        local.save(studying.toDto())
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
        onSuccess = { Result.Success(it?.toDomain()) },
        onFailure = { e ->
            Log.e("studying", "학습중 세션 읽기", e)
            Result.Failure(AppError.Local("불러오기 실패"))
        }
    )

    override suspend fun clearLocalSession() = safeRunCatching {
        local.clear()
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = {e ->
            Log.e("studying", "세션 초기화", e)
            Result.Failure(AppError.Local("세션 초기화 실패"))
        }
    )

    override suspend fun saveLocalGoalDraft(draft: StudyingGoalDraft): Result<Unit> = safeRunCatching {
        local.saveGoalDraft(draft.toDto())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("studying", "학습 목표 임시 글 저장", e)
            Result.Failure(AppError.Local("임시 저장 실패"))
        }
    )

    override suspend fun readLocalGoalDraft(): Result<StudyingGoalDraft?> = safeRunCatching {
        local.readGoalDraft()
    }.fold(
        onSuccess = { Result.Success(it?.toDomain()) },
        onFailure = { e ->
            Log.e("studying", "학습 목표 임시 글 읽기", e)
            Result.Failure(AppError.Local("불러오기 실패"))
        }
    )

    override suspend fun clearLocalGoalDraft(): Result<Unit> = safeRunCatching {
        local.clearGoalDraft()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("studying", "학습 목표 임시 글 삭제", e)
            Result.Failure(AppError.Local("임시 글 삭제 실패"))
        }
    )

    companion object {
        /** 학습 기록 저장 제한 시간 — 오프라인에서는 쓰기가 끝나지 않으므로 이 시간만 기다리고 Pending으로 처리 */
        val SAVE_TIMEOUT = 5.seconds

        /** 홈 복구 창 판단 전 대기열이 비기를 기다리는 시간 — 홈이 보일 때마다 실행되므로 짧게 둠 */
        val PENDING_WRITES_TIMEOUT = 3.seconds

        /** 사유를 찾지 못한 저장 거부 안내 — 백업은 남아 홈 복구 창에서 다시 저장할 수 있다 */
        private const val UNRESOLVED_REJECTION_MESSAGE =
            "학습 기록을 저장하지 못했습니다. 기기 시간이 실제보다 빠르면 잠시 후 다시 저장해 주세요."
    }
}
