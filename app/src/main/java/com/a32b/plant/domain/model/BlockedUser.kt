package com.a32b.plant.domain.model

/**
 * 내가 차단한 사용자 (users/{내 uid}/blocks/{uid}, #660)
 *
 * @param nickname 차단한 시점의 닉네임 — 차단 목록 화면 표시용. 상대가 닉네임을 바꿔도 갱신하지 않는다
 * @param profileImg 차단한 시점의 프로필 이미지 레벨
 * @param createdAt 차단한 시각. 서버에 반영되기 전이면 기기 추정 시각.
 *   차단할 대상([create])이거나, createdAt이 없거나 형식이 맞지 않는 문서(uid만 남긴 경우 포함)면 null
 */
data class BlockedUser(
    val uid: String,
    val nickname: String,
    val profileImg: String,
    val createdAt: Long?
) {
    companion object {
        /** 차단할 대상 — 시각은 서버가 정한다 */
        fun create(uid: String, nickname: String, profileImg: String) = BlockedUser(
            uid = uid,
            nickname = nickname,
            profileImg = profileImg,
            createdAt = null
        )
    }
}
