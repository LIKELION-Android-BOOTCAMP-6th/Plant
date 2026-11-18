package com.a32b.plant.domain.type

/**
 * 게시글·댓글 신고 사유. 선언 순서가 신고 시트의 표시 순서이며, "기타"는 항상 마지막에 둔다.
 *
 * @property firestoreKey `reports/{reportId}.reason`에 저장되는 값. 이미 저장된 신고가 있으므로 기존 값은 바꾸지 않는다.
 * @property allowsDetail 상세 내용 입력란을 여는 사유인지 여부
 */
enum class ReportReason(
    val label: String,
    val firestoreKey: String,
    val allowsDetail: Boolean = false
) {
    ABUSE("욕설/비방", "abuse"),
    SPAM("스팸/광고", "spam"),
    INAPPROPRIATE("부적절한 콘텐츠", "inappropriate"),
    // 운영진이 원문과 비교해 확인할 수 있도록 출처를 적을 수 있게 입력란을 연다 (#638)
    COPYRIGHT("저작권 등 권리 침해", "copyright", allowsDetail = true),
    OTHER("기타", "other", allowsDetail = true)
}
