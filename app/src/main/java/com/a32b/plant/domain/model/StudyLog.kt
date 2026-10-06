package com.a32b.plant.domain.model


data class StudyLog(
    val title: String,
    val contents: List<String>,
    val studyingTime: Long,
    val createAt: Long?,
    val startedAt: Long,
    val id: String,
    val isSelected: Boolean
){
    companion object{
        fun write(title: String, contents: List<String>, time: Long, startedAt: Long) = StudyLog(
            title = title,
            contents = contents,
            studyingTime = time,
            createAt = null,
            startedAt = startedAt,
            id = "",
            isSelected = false
        )
    }

}
