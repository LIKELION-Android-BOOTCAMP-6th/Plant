package com.a32b.plant.fake

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyLogRef
import com.a32b.plant.domain.model.StudyRewardStatus
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingGoalDraft
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.model.UnseenStudyReward
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

/** 테스트 공용 [StudyingRepository]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다. */
class FakeStudyingRepository : StudyingRepository {
    var clearLocalSessionCalls = 0

    override suspend fun clearLocalSession(): Result<Unit> {
        clearLocalSessionCalls++
        return Result.Success(Unit)
    }

    override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> = error("Unused")
    override suspend fun saveStudyingUser(user: StudyingUser): Result<Unit> = error("Unused")
    override suspend fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLog): Result<StudySaveResult> = error("Unused")
    override suspend fun awaitPendingWrites(): Boolean = error("Unused")
    override suspend fun isStudySessionSaved(uid: String, potId: String, sessionId: String): Result<Boolean> = error("Unused")
    override fun observeStudyReward(uid: String, potId: String, sessionId: String): Flow<StudyRewardStatus?> = error("Unused")
    override fun observeUnseenStudyRewards(uid: String): Flow<List<UnseenStudyReward>> = error("Unused")
    override suspend fun markStudyRewardsSeen(uid: String, refs: List<StudyLogRef>): Result<Unit> = error("Unused")
    override suspend fun deleteStudyingUserInfo(): Result<Unit> = error("Unused")
    override suspend fun saveLocalSession(studying: StudyingSession): Result<Unit> = error("Unused")
    override suspend fun readLocalSession(): Result<StudyingSession?> = error("Unused")
    override suspend fun saveLocalGoalDraft(draft: StudyingGoalDraft): Result<Unit> = error("Unused")
    override suspend fun readLocalGoalDraft(): Result<StudyingGoalDraft?> = error("Unused")
    override suspend fun clearLocalGoalDraft(): Result<Unit> = error("Unused")
}
