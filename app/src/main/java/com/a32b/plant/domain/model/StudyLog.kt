package com.a32b.plant.domain.model


data class StudyLog(
    val title: String,
    val contents: List<String>,
    val completedContents: List<Boolean>,
    val studyingTime: Long,
    val createAt: Long?,
    val startedAt: Long,
    val id: String,
    val isSelected: Boolean
){
    companion object{
        /** 세션 하나의 최대 학습 시간 (24시간) — 보안 규칙(firestore.rules maxStudyingTimeMs)과 같은 값 */
        const val MAX_STUDYING_TIME_MS = 24 * 60 * 60 * 1000L

        /** 오프라인 동기화를 허용하는 기간 (일) — 보안 규칙(firestore.rules maxOfflineSyncDays)과 같은 값 (#633) */
        const val MAX_OFFLINE_SYNC_DAYS = 7L

        private const val MAX_OFFLINE_SYNC_MS = MAX_OFFLINE_SYNC_DAYS * 24 * 60 * 60 * 1000L

        /**
         * 시작 시각이 [nowMillis]보다 오프라인 동기화 허용 기간 넘게 과거인지 — 규칙이 거부할 기록 (#633)
         * 시작 시각과 현재 시각을 같은 기기 시계로 재므로, 기기 시계가 일정하게 틀려도 결과는 실제 경과 시간 기준이다
         */
        fun isTooOldToSave(startedAt: Long, nowMillis: Long): Boolean =
            nowMillis - startedAt > MAX_OFFLINE_SYNC_MS

        fun write(title: String, contents: List<String>, completedContents: List<Boolean>, time: Long, startedAt: Long) = StudyLog(
            title = title,
            contents = contents,
            completedContents = completedContents,
            studyingTime = time,
            createAt = null,
            startedAt = startedAt,
            id = "",
            isSelected = false
        )
    }

}
