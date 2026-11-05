package com.a32b.plant.data.source.local

import com.a32b.plant.data.model.StudyingGoalDraftDto
import com.a32b.plant.data.model.StudyingSessionDto

interface StudyingLocalDataSource {
    suspend fun save(studying: StudyingSessionDto)

    suspend fun read(): StudyingSessionDto?

    /** 세션 백업과 목표 임시 글을 모두 삭제 */
    suspend fun clear()

    /** 목표 입력 창 임시 글 저장/읽기/삭제 — 세션 백업 값은 건드리지 않음 */
    suspend fun saveGoalDraft(draft: StudyingGoalDraftDto)

    suspend fun readGoalDraft(): StudyingGoalDraftDto?

    suspend fun clearGoalDraft()
}
