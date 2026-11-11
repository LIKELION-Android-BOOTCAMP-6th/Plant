package com.a32b.plant.data.source.remote.user

import com.a32b.plant.data.model.AttendanceResultDto
import com.a32b.plant.data.model.AttendanceRewardDto
import com.a32b.plant.data.model.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 출석 함수 응답 대기 시간. 기본값(70초)이면 요청 중 연결이 끊겼을 때 출석 다이얼로그가
// 처리 중 상태로 잠겨 오래 닫을 수 없다. 콜드 스타트(운영 미측정)에도 여유가 있으면서 기본값(70초)보다 충분히 짧게 둔다.
// 시간 초과는 AppError.Network로 바뀌고, 서버에서 처리됐다면 출석판은 유저 문서 구독으로 갱신된다
private const val ATTENDANCE_TIMEOUT_SECONDS = 20L

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : UserRemoteDataSource {

    override suspend fun getUser(uid: String): UserDto? {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(UserDto::class.java)
    }

    override suspend fun getUserFromServer(uid: String): UserDto? {
        val snapshot = db.collection("users").document(uid).get(Source.SERVER).await()
        return snapshot.toObject(UserDto::class.java)
    }

    override suspend fun getUserFromCache(uid: String): UserDto? {
        val snapshot = db.collection("users").document(uid).get(Source.CACHE).await()
        return snapshot.toObject(UserDto::class.java)
    }

    override fun observeUser(uid: String): Flow<UserDto?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserDto::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createUser(uid: String, user: UserDto) {
        db.collection("users").document(uid).set(user).await()
    }

    // 닉네임 등록(nicknames)과 첫 로그인 완료(users)를 한 트랜잭션으로 처리한다.
    // 일반 write는 오프라인에서 서버 응답이 올 때까지 await()가 끝나지 않고 로컬 큐에 남지만,
    // 트랜잭션은 오프라인이면 실패하고, 둘 중 하나만 반영되는 상태도 만들지 않는다.
    // 확인한 서버 상태만 반환하고, 이를 성공/실패로 해석하는 일은 Repository가 한다.
    // (중복을 예외로 던지면 Firestore가 일부 코드를 자동 재시도해 응답이 늦어지므로 값으로 반환한다)
    override suspend fun completeFirstLogin(uid: String, nickname: String): CompleteFirstLoginResult {
        val nicknameRef = db.collection("nicknames").document(nickname)
        val userRef = db.collection("users").document(uid)

        return db.runTransaction { txn ->
            val userSnapshot = txn.get(userRef)
            if (userSnapshot.getString("nickname") == nickname &&
                userSnapshot.getBoolean("isFirstLogin") == false
            ) {
                return@runTransaction CompleteFirstLoginResult.AlreadyCompleted
            }

            if (txn.get(nicknameRef).exists()) {
                return@runTransaction CompleteFirstLoginResult.NicknameTaken
            }

            txn.set(nicknameRef, mapOf("nickname" to nickname, "uid" to uid))
            txn.update(
                userRef,
                "nickname", nickname,
                "isFirstLogin", false
            )
            CompleteFirstLoginResult.Completed
        }.await()
    }

    // 닉네임 변경(중복 확인·새 닉네임 문서 생성·이전 닉네임 문서 삭제)과 users 수정을 한 트랜잭션으로 처리한다.
    // 일반 write는 오프라인에서 서버 응답이 올 때까지 await()가 끝나지 않고 로컬 큐에 남지만,
    // 트랜잭션은 오프라인이면 서버에 쓰지 못하고 기기 캐시에도 반영되지 않으며, 일부만 반영되는 상태도 만들지 않는다.
    // 다만 SDK가 UNAVAILABLE을 최대 5회까지 재시도한 뒤 실패하므로 즉시 실패하지는 않고, Repository의 10초 제한에서 끊긴다.
    // 이전 닉네임은 앱이 들고 있는 값이 아니라 서버의 users 문서 값을 기준으로 지운다.
    // 확인한 서버 상태만 반환하고, 성공/실패로 해석하는 일은 Repository가 한다.
    // (중복을 예외로 던지면 Firestore가 일부 코드를 자동 재시도해 응답이 늦어지므로 값으로 반환한다)
    override suspend fun updateProfile(
        uid: String,
        nickname: String,
        profileImg: String
    ): UpdateProfileResult {
        val userRef = db.collection("users").document(uid)

        return db.runTransaction { txn ->
            val userSnapshot = txn.get(userRef)
            val currentNickname = userSnapshot.getString("nickname").orEmpty()
            val currentProfileImg = userSnapshot.getString("profileImg")

            if (currentNickname == nickname && currentProfileImg == profileImg) {
                return@runTransaction UpdateProfileResult.NoChange
            }

            // 닉네임은 그대로, 이미지만 바꾸는 경우
            if (currentNickname == nickname) {
                txn.update(userRef, "profileImg", profileImg)
                return@runTransaction UpdateProfileResult.Updated
            }

            // 트랜잭션에서는 읽기를 쓰기보다 먼저 모두 끝내야 한다
            val newNicknameRef = db.collection("nicknames").document(nickname)
            if (txn.get(newNicknameRef).exists()) {
                return@runTransaction UpdateProfileResult.NicknameTaken
            }

            txn.set(newNicknameRef, mapOf("nickname" to nickname, "uid" to uid))
            if (currentNickname.isNotEmpty()) {
                txn.delete(db.collection("nicknames").document(currentNickname))
            }
            txn.update(
                userRef,
                "nickname", nickname,
                "profileImg", profileImg
            )
            UpdateProfileResult.Updated
        }.await()
    }

    override suspend fun checkAttendance(): AttendanceResultDto {
        // 사용자는 서버가 로그인 토큰으로 확인한다 (uid를 보내지 않음)
        val result = functions.getHttpsCallable("checkAttendance")
            .withTimeout(ATTENDANCE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .call()
            .await()
        val data = result.getData() as? Map<*, *>
        val reward = data?.get("reward") as? Map<*, *>
        return AttendanceResultDto(
            status = data?.get("status") as? String ?: "",
            newCount = (data?.get("newCount") as? Number)?.toInt(),
            reward = reward?.let {
                AttendanceRewardDto(
                    item = it["item"] as? String,
                    amount = (it["amount"] as? Number)?.toInt(),
                    gold = (it["gold"] as? Number)?.toInt()
                )
            }
        )
    }

    override suspend fun updateSessionToken(uid: String, token: String?) {
        db.collection("users").document(uid)
            .update("activeSessionToken", token)
            .await()
    }

}
