package com.a32b.plant.data.source.remote.block

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.model.BlockedUserDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : BlockRemoteDataSource {

    private fun blocks(uid: String) =
        db.collection("users").document(uid).collection("blocks")

    // 정렬은 하지 않는다 — 오프라인에서 방금 쓴 문서는 createdAt(서버 시각)이 아직 없어 orderBy 결과에서 위치가 흔들릴 수 있음.
    // 정렬은 Repository가 받은 뒤 한다
    override fun observeBlockedUsers(uid: String): Flow<List<BlockedUserDto>> = callbackFlow {
        val listener = blocks(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { doc ->
                // 방금 쓴 문서는 서버 시각이 아직 정해지지 않아 기본값이면 null → 로컬 추정 시각을 받는다
                // 형식이 맞지 않는 문서(콘솔에서 직접 고친 경우 등) 하나 때문에 콜백에서 앱이 종료되지 않게 한다.
                // 그 문서를 빼면 차단이 풀린 것처럼 보이므로 문서 ID(차단한 uid)만이라도 남긴다
                safeRunCatching {
                    doc.toObject(BlockedUserDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.getOrElse { e ->
                    Log.e("BlockRemoteDataSource", "차단 문서 변환 실패: ${doc.id}", e)
                    BlockedUserDto(uid = doc.id)
                }
            } ?: emptyList()
            trySend(users)
        }
        awaitClose { listener.remove() }
    }

    // 문서 ID를 차단한 uid로 고정 → 다시 차단해도 문서가 늘지 않고 덮어쓴다 (보안 규칙이 허용 필드·자기 자신 차단을 검사)
    // blockedUid 필드: 문서 ID와 같은 값. 상대가 탈퇴할 때 서버(deleteUserData)가 이 필드로 찾아 지운다 (#779, 규칙이 문서 ID와 같은지 검사)
    override fun blockUser(uid: String, blockedUid: String, nickname: String, profileImg: String): Task<Void> =
        blocks(uid).document(blockedUid).set(
            mapOf(
                "blockedUid" to blockedUid,
                "nickname" to nickname,
                "profileImg" to profileImg,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )

    override fun unblockUser(uid: String, blockedUid: String): Task<Void> =
        blocks(uid).document(blockedUid).delete()
}
