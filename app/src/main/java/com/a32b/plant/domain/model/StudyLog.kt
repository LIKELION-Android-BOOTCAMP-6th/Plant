package com.a32b.plant.domain.model


data class StudyLog(
    val title: String,
    val contents: List<String>,
    val completedContents: List<Boolean> = emptyList(),
    val studyingTime: Long,
    val createAt: Long?,
    val startedAt: Long,
    val id: String,
    val isSelected: Boolean
){
    companion object{
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
