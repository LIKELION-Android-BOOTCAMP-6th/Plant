package com.a32b.plant.domain.community

import kotlinx.coroutines.flow.Flow

// 현재 로그인한 사용자의 상호작용(좋아요/북마크/댓글/게시글 수정/신고) 결과가 전파된다.
// 상세 화면 재조회 후에는 서버 값을 통째로 전달하므로, 그 시점에 반영된 다른 사용자의 변경(신고 누적·좋아요 수 등)도 함께 실린다.
// 그 외 다른 사용자의 변경은 이 버스를 타지 않으며, 각 화면 자체의 refresh/silentRefresh로만 보정된다.
interface PostUpdateObserver {
    val event: Flow<PostUpdate>
}
