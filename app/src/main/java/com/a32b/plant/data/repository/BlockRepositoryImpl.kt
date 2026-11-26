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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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
            ?: createSharedBlockedUsers(uid).also { shared = uid to it }
    }

    private fun createSharedBlockedUsers(uid: String): StateFlow<List<BlockedUser>?> {
        // 이 uid의 목록을 한 번이라도 받았는지 — flow {} 밖에 둬서, 구독하는 화면이 모두 떠났다가 리스너를 다시 시작해도
        // 들고 있던 목록을 첫 목록 전 실패로 오인해 빈 목록으로 덮지 않게 한다
        val received = AtomicBoolean(false)
        return flow {
            remote.observeBlockedUsers(uid)
                .map { list ->
                    // 최근에 차단한 순서. 방금 차단한 문서는 기기 추정 시각으로 읽힌다(RemoteDataSource의 ESTIMATE).
                    // createdAt이 없거나 형식이 맞지 않는 문서(uid만 남긴 경우 포함)는 맨 위
                    list.map { it.toDomain() }.sortedByDescending { it.createdAt ?: Long.MAX_VALUE }
                }
                .onEach { received.set(true) }
                // 앱 범위 스코프 밖으로 전파되면 앱이 죽으므로 흡수한다.
                // 이미 받은 목록이 있으면 그대로 유지하고, 첫 목록 전에 실패했으면(보안 규칙 거부 등) 빈 목록을 한 번 보낸다
                // → 첫 목록을 기다리는 화면(목록·상세·차단 목록)이 열 때마다 대기 시간까지 기다리지 않게 한다
                .catch { e ->
                    Log.e("BlockRepository", "차단 목록 구독 실패: ${e.message}", e)
                    if (!received.get()) emit(emptyList())
                }
                .collect { emit(it) }
        }
            // 구독하는 화면이 없어지면 바로 리스너를 멈춘다(0초). 마지막 목록은 그대로 들고 있어 다시 구독하면 바로 받는다.
            // 멈추는 시간을 두면, 로그아웃(인증이 끊겨 리스너가 실패)한 뒤 그 시간 안에 같은 계정으로 다시 로그인했을 때
            // 끝난 리스너를 다시 시작하지 않아 그 세션 동안 차단이 반영되지 않는다 (#660)
            .stateIn(scope, SharingStarted.WhileSubscribed(0), null)
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
            // 규칙 거부(탈퇴한 사용자 차단 등, #779)도 나머지와 같은 안내
            else -> AppError.Custom("${action}에 실패했어요.")
        }
        else -> AppError.Unknown()
    }

    companion object {
        /** 다른 커뮤니티 쓰기(CommunityRepositoryImpl.WRITE_TIMEOUT)와 같은 5초 */
        private val WRITE_TIMEOUT = 5.seconds
    }
}
