package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** users/{내 uid}/blocks/{차단한 uid} 문서 (#660). 문서 ID가 차단한 사용자의 uid */
data class BlockedUserDto(
    @DocumentId
    var uid: String = "",
    var nickname: String = "",
    var profileImg: String = "",
    // 쓸 때는 RemoteDataSource가 serverTimestamp()로 채운다 (보안 규칙이 request.time과 같은지 검사)
    var createdAt: Timestamp? = null
)
