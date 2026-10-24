package com.a32b.plant.domain.type

enum class ReportReason(val label: String, val firestoreKey: String) {
    ABUSE("욕설/비방", "abuse"),
    SPAM("스팸/광고", "spam"),
    INAPPROPRIATE("부적절한 콘텐츠", "inappropriate"),
    OTHER("기타", "other")
}
