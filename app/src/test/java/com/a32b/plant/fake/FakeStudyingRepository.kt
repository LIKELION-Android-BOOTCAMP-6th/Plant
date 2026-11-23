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
import java.time.LocalDate

/** 테스트 공용 [StudyingRepository]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다. */
class FakeStudyingRepository(
    var saveStudySessionResult: Result<StudySaveResult> = Result.Success(StudySaveResult.Saved),
    var saveLocalSessionResult: Result<Unit> = Result.Success(Unit),
    /** observeStudyingUser가 돌려줄 Flow — 넣지 않으면 호출 시 실패 */
    var studyingUsers: Flow<List<StudyingUser>>? = null
) : StudyingRepository {
    var clearLocalSessionCalls = 0

    /** saveStudySession으로 보낸 학습 기록 (보낸 순서대로) */
    val savedStudyLogs = mutableListOf<StudyLog>()

    /** saveLocalSession으로 저장한 백업 (저장한 순서대로) */
    val savedLocalSessions = mutableListOf<StudyingSession>()

    override suspend fun clearLocalSession(): Result<Unit> {
        clearLocalSessionCalls++
        return Result.Success(Unit)
    }

    override suspend fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLog): Result<StudySaveResult> {
        savedStudyLogs += log
        return saveStudySessionResult
    }

    override suspend fun saveLocalSession(studying: StudyingSession): Result<Unit> {
        savedLocalSessions += studying
        return saveLocalSessionResult
    }

    /** updateLocalSessionTime으로 보낸 (세션 ID, 학습 시간) (보낸 순서대로) */
    val updatedLocalSessionTimes = mutableListOf<Pair<String, Long>>()

    override suspend fun updateLocalSessionTime(sessionId: String, time: Long): Result<Unit> {
        updatedLocalSessionTimes += sessionId to time
        return Result.Success(Unit)
    }

    /** readNotificationGuideShownDate가 돌려줄 값 */
    var notificationGuideShownDateResult: Result<LocalDate?> = Result.Success(null)
    var saveNotificationGuideShownDateResult: Result<Unit> = Result.Success(Unit)

    /** saveNotificationGuideShownDate로 저장한 날짜 (저장한 순서대로) */
    val savedNotificationGuideShownDates = mutableListOf<LocalDate>()

    override suspend fun readNotificationGuideShownDate(): Result<LocalDate?> = notificationGuideShownDateResult

    override suspend fun saveNotificationGuideShownDate(date: LocalDate): Result<Unit> {
        savedNotificationGuideShownDates += date
        return saveNotificationGuideShownDateResult
    }

    override fun observeStudyingUser(tag: String): Flow<List<StudyingUser>> = studyingUsers ?: error("Unused")
    override suspend fun saveStudyingUser(user: StudyingUser): Result<Unit> = error("Unused")
    override suspend fun awaitPendingWrites(): Boolean = error("Unused")
    override suspend fun isStudySessionSaved(uid: String, potId: String, sessionId: String): Result<Boolean> = error("Unused")
    override fun observeStudyReward(uid: String, potId: String, sessionId: String): Flow<StudyRewardStatus?> = error("Unused")
    override fun observeUnseenStudyRewards(uid: String): Flow<List<UnseenStudyReward>> = error("Unused")
    override suspend fun markStudyRewardsSeen(uid: String, refs: List<StudyLogRef>): Result<Unit> = error("Unused")
    override suspend fun deleteStudyingUserInfo(): Result<Unit> = error("Unused")
    override suspend fun readLocalSession(): Result<StudyingSession?> = error("Unused")
    override suspend fun saveLocalGoalDraft(draft: StudyingGoalDraft): Result<Unit> = error("Unused")
    override suspend fun readLocalGoalDraft(): Result<StudyingGoalDraft?> = error("Unused")
    override suspend fun clearLocalGoalDraft(): Result<Unit> = error("Unused")
}
