package com.a32b.plant.presentation.community

object CommunityConstants {
    const val POST_PAGE_SIZE = 20
    // 블라인드(Post.isBlind / Comment.isBlind)된 게시글 본문·댓글 내용 대신 표시하는 문구
    const val BLIND_CONTENT_MESSAGE = "신고가 접수되어 숨김 처리된 콘텐츠예요."
    // 블라인드된 게시글의 제목 대신 표시하는 문구 — 목록 카드 제목이 한 줄이라 짧게 유지
    const val BLIND_TITLE_MESSAGE = "숨김 처리된 게시글"
    // 블라인드 게시글의 댓글 입력창 차단 안내 — 입력창 placeholder·접근성 설명·Toast 공통
    const val BLIND_POST_COMMENT_BLOCKED_MESSAGE = "숨김 처리된 게시글에는 댓글을 달 수 없어요"

    // 커뮤니티 쓰기는 온라인 전용 (#641) — 오프라인이면 쓰기를 보내지 않고 이 안내만 표시
    const val OFFLINE_WRITE_MESSAGE = "인터넷에 연결된 뒤 다시 시도해 주세요"
    // 온라인 판정이었지만 제한 시간 안에 서버 응답이 없을 때 (쓰기는 대기열에 남아 연결되면 전송됨)
    const val WRITE_PENDING_MESSAGE = "네트워크가 불안정해요. 연결되면 자동으로 반영돼요"
    // 오프라인일 때 쓰기 버튼을 비활성처럼 보이게 하는 값 — 출석 버튼(AttendanceCheckDialog)과 같은 값
    const val OFFLINE_BUTTON_ALPHA = 0.38f
    // alpha는 시각 표시뿐이라 TalkBack에 따로 알리는 상태 설명
    const val OFFLINE_STATE_DESCRIPTION = "오프라인"
}
