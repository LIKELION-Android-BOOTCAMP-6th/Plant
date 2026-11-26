package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** users/{내 uid}/blocks/{차단한 uid} 문서 (#660). 문서 ID가 차단한 사용자의 uid */
data class BlockedUserDto(
    @DocumentId
    var uid: String = "",
    // 문서 ID와 같은 값 (#779, 탈퇴 시 서버가 찾는 용도). 화면에는 문서 ID(uid)를 쓴다
    // 필드를 두지 않으면 읽을 때마다 Firestore가 "No setter/field" 경고를 남긴다
    var blockedUid: String = "",
    var nickname: String = "",
    var profileImg: String = "",
    // 쓸 때는 RemoteDataSource가 serverTimestamp()로 채운다 (보안 규칙이 request.time과 같은지 검사)
    var createdAt: Timestamp? = null
)
