package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyLogRef
import com.a32b.plant.domain.model.StudyRewardStatus
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingGoalDraft
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.model.UnseenStudyReward
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface StudyingRepository {

    /** 같은 태그로 학습 중인 사용자 구독*/
    fun observeStudyingUser(tag: String): Flow<List<StudyingUser>>

    /**
     * "공부 중" 표시(studying/{uid}) 등록·갱신 — 공부 시작·재개 시와 공부 중 1분마다 호출
     * 문서가 없어도(일시정지로 삭제, 예약 함수가 정리) 다시 만든다 (#525)
     */
    suspend fun saveStudyingUser(user: StudyingUser): Result<Unit>

    /**
     * 학습 기록 저장 — 기록(logs/{sessionId}) 생성과 화분·유저 학습 시간 누적을 batch 하나로 쓴다 (#453)
     * 오프라인이면 기기 캐시에 바로 반영되고 [StudySaveResult.Pending]을 반환한다 (연결되면 전송됨, 재전송 금지)
     * 보안 규칙이 거부하면 서버 상태를 조회해 [StudySaveResult.AlreadySaved] / [StudySaveResult.Rejected]로 구분한다
     * 사유를 찾지 못한 거부는 시간이 지나면 통과할 수 있어(기기 시계가 빠름 등) [Result.Failure]로 돌려준다 → 호출부는 백업 유지
     */
    suspend fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLog): Result<StudySaveResult>

    /**
     * 지금까지 보낸 쓰기가 서버에서 처리될 때까지 잠시 기다린다
     * false(오프라인 등)면 서버 상태로 판단하지 말 것 — 대기열에 저장 요청이 남아 있을 수 있음
     */
    suspend fun awaitPendingWrites(): Boolean

    /** 서버에 이 세션의 학습 기록이 있는지 (캐시가 아닌 서버 조회) */
    suspend fun isStudySessionSaved(uid: String, potId: String, sessionId: String): Result<Boolean>

    /**
     * 학습 기록 하나의 보상 지급 상태 구독 — 기록이 없으면(아직 저장 전, 거부돼 되돌아감, 삭제) null
     * 리스너 에러는 Flow 예외로 전달되므로 호출부에서 catch
     */
    fun observeStudyReward(uid: String, potId: String, sessionId: String): Flow<StudyRewardStatus?>

    /**
     * 지급됐지만 아직 확인하지 않은 학습 보상 구독 (rewardStatus == granted && rewardSeen == false)
     * 리스너 에러는 Flow 예외로 전달되므로 호출부에서 catch
     */
    fun observeUnseenStudyRewards(uid: String): Flow<List<UnseenStudyReward>>

    /** 보상을 확인했다고 표시 (rewardSeen = true) — 오프라인이면 기기 캐시에 먼저 반영되고 연결되면 전송 */
    suspend fun markStudyRewardsSeen(uid: String, refs: List<StudyLogRef>): Result<Unit>

    /** 학습 종료 시 유저 정보 삭제 */
    suspend fun deleteStudyingUserInfo(): Result<Unit>

    /** 로컬 DB 저장/읽기/삭제 */
    suspend fun saveLocalSession(studying : StudyingSession): Result<Unit>

    suspend fun readLocalSession(): Result<StudyingSession?>

    suspend fun clearLocalSession(): Result<Unit>

    /** 목표 입력 창 임시 글 로컬 저장/읽기/삭제 */
    suspend fun saveLocalGoalDraft(draft: StudyingGoalDraft): Result<Unit>

    suspend fun readLocalGoalDraft(): Result<StudyingGoalDraft?>

    suspend fun clearLocalGoalDraft(): Result<Unit>

}
