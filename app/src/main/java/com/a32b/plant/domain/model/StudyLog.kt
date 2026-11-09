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
