package com.a32b.plant.data.source.local

import com.a32b.plant.data.model.StudyingGoalDraftDto
import com.a32b.plant.data.model.StudyingSessionDto

interface StudyingLocalDataSource {
    suspend fun save(studying: StudyingSessionDto)

    suspend fun read(): StudyingSessionDto?

    /**
     * 학습 시간(time)만 갱신 — 백업이 같은 세션(sessionId)이고 상태가 [studyingStatus]이며 일시정지가 아닐 때만 쓴다
     * 확인과 쓰기를 한 번의 edit 안에서 해서, 그사이 다른 저장(종료·삭제·일시정지)이 끼어도 그 결과를 덮지 않는다
     * 백업이 없거나 조건이 맞지 않으면 아무것도 쓰지 않는다 (새 백업을 만들지 않음)
     */
    suspend fun updateTime(sessionId: String, time: Long, studyingStatus: String)

    /** 세션 백업과 목표 임시 글을 모두 삭제 */
    suspend fun clear()

    /** 목표 입력 창 임시 글 저장/읽기/삭제 — 세션 백업 값은 건드리지 않음 */
    suspend fun saveGoalDraft(draft: StudyingGoalDraftDto)

    suspend fun readGoalDraft(): StudyingGoalDraftDto?

    suspend fun clearGoalDraft()
}
