package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.firestore.WriteResult
import com.a32b.plant.data.firestore.awaitServerWrite
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.source.remote.block.BlockRemoteDataSource
import com.a32b.plant.di.qualifier.ApplicationScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.BlockRepository
import com.a32b.plant.domain.result.Result
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val remote: BlockRemoteDataSource,
    @param:ApplicationScope private val scope: CoroutineScope
) : BlockRepository {

    // 지금 로그인한 uid의 차단 목록 하나만 공유한다. 다른 uid를 구독하면(계정 전환) 새로 만든다.
    // null = 아직 첫 목록을 받지 못함
    private var shared: Pair<String, StateFlow<List<BlockedUser>?>>? = null
    private val lock = Any()

    override fun observeBlockedUsers(uid: String): Flow<List<BlockedUser>> =
        sharedBlockedUsers(uid).filterNotNull()

    private fun sharedBlockedUsers(uid: String): StateFlow<List<BlockedUser>?> = synchronized(lock) {
        shared?.takeIf { it.first == uid }?.second
            ?: remote.observeBlockedUsers(uid)
                .map { list ->
                    // 최근에 차단한 순서. 서버 시각이 아직 없는 문서(방금 차단)는 맨 위
                    list.map { it.toDomain() }.sortedByDescending { it.createdAt ?: Long.MAX_VALUE }
                }
                // 로그아웃·탈퇴로 인증이 먼저 끊기면 PERMISSION_DENIED가 올 수 있다.
                // 앱 범위 스코프 밖으로 전파되면 앱이 죽으므로 흡수하고 마지막 목록을 유지한다
                .catch { e -> Log.e("BlockRepository", "차단 목록 구독 실패: ${e.message}", e) }
                // 화면을 오가는 사이(5초)에는 리스너를 유지해 다시 받지 않는다
                .stateIn(scope, SharingStarted.WhileSubscribed(SHARE_STOP_TIMEOUT_MS), null)
                .also { shared = uid to it }
    }

    override suspend fun blockUser(uid: String, target: BlockedUser): Result<SaveResult> =
        awaitWrite("차단") {
            remote.blockUser(uid, target.uid, target.nickname, target.profileImg)
        }

    override suspend fun unblockUser(uid: String, blockedUid: String): Result<SaveResult> =
        awaitWrite("차단 해제") {
            remote.unblockUser(uid, blockedUid)
        }

    // 쓰기를 시작하고 서버 반영을 제한 시간만큼만 기다린다. 응답이 없으면(오프라인) 대기열에 남아 연결되면 전송되므로 다시 보내지 않는다
    private suspend fun awaitWrite(action: String, write: () -> Task<Void>): Result<SaveResult> =
        safeRunCatching { write().awaitServerWrite(WRITE_TIMEOUT) }.fold(
            onSuccess = { result ->
                when (result) {
                    WriteResult.Confirmed -> Result.Success(SaveResult.Saved)
                    WriteResult.Pending -> Result.Success(SaveResult.Pending)
                    is WriteResult.Rejected -> {
                        Log.e("BlockRepository", "$action 거부", result.error)
                        Result.Failure(handleError(result.error, action))
                    }
                }
            },
            onFailure = { e ->
                Log.e("BlockRepository", "$action 실패", e)
                Result.Failure(handleError(e, action))
            }
        )

    private fun handleError(e: Throwable, action: String): AppError = when (e) {
        is AppError -> e
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
            // 정상 사용 중에는 규칙이 막는 쓰기(자기 자신 차단 등)를 보내지 않으므로 나머지와 같은 안내
            else -> AppError.Custom("${action}에 실패했어요.")
        }
        else -> AppError.Unknown()
    }

    companion object {
        /** 다른 커뮤니티 쓰기(CommunityRepositoryImpl.WRITE_TIMEOUT)와 같은 5초 */
        private val WRITE_TIMEOUT = 5.seconds
        private const val SHARE_STOP_TIMEOUT_MS = 5_000L
    }
}
