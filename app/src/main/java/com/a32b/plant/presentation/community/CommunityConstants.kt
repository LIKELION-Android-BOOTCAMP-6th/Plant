package com.a32b.plant.presentation.community

object CommunityConstants {
    const val POST_PAGE_SIZE = 20
    // 블라인드(Post.isBlind / Comment.isBlind)된 게시글 본문·댓글 내용 대신 표시하는 문구
    const val BLIND_CONTENT_MESSAGE = "신고가 접수되어 숨김 처리된 콘텐츠입니다."
    // 블라인드된 게시글의 제목 대신 표시하는 문구 — 목록 카드 제목이 한 줄이라 짧게 유지
    const val BLIND_TITLE_MESSAGE = "숨김 처리된 게시글"
}
