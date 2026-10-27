package com.a32b.plant.domain.community

import kotlinx.coroutines.flow.Flow

// 현재 로그인한 사용자가 직접 수행한 변경(좋아요/북마크/댓글/게시글 수정)만 전파된다.
// 다른 사용자가 변경한 내용은 이 버스를 타지 않으며, 각 화면 자체의 refresh/silentRefresh로만 보정된다.
interface PostUpdateObserver {
    val event: Flow<PostUpdate>
}
