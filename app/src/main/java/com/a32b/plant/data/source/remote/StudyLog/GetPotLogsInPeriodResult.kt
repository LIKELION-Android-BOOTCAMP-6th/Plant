package com.a32b.plant.data.datasource

import com.a32b.plant.data.model.StudyLogDto

data class GetPotLogsInPeriodResult(val logs: List<StudyLogDto>, val isFromCache: Boolean)
