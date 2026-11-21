package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 같은 태그로 공부 중인 사용자 구독 — 유령(강제 종료로 남은 문서)을 빼고 상위 [MAX_DISPLAY_COUNT]명만 보낸다 (#618)
 *
 * 공부 중인 앱은 1분마다 studying/{uid}의 updatedAt을 서버 시각으로 갱신한다 (StudyingViewModel.observeTimer).
 * 강제 종료된 사용자의 문서는 서버 예약 함수(functions/index.js cleanUpStaleStudying)가 지우지만
 * 지우기 전까지 최대 약 20분 남으므로, 그 사이 화면에서 숨긴다.
 * - updatedAt이 [STALE_THRESHOLD_MS]보다 오래됐거나 없으면 뺀다 (없으면 오래된 것으로 보는 건 서버 isStaleStudying과 같음)
 * - 유령은 문서를 바꾸지 않아 리스너가 다시 불리지 않으므로, [REFILTER_INTERVAL_MS]마다 마지막 목록을 다시 거른다
 *   (일시정지 중에도 목록이 보이므로 공부 중일 때만 도는 observeTimer가 아니라 여기서 돈다)
 * - updatedAt은 서버 시각, 비교하는 현재 시각은 기기 시각이라 기기 시계가 기준 시간 이상 빠르면 공부 중인 사람도 숨겨질 수 있다
 */
class ObserveStudyingUsersUseCase(
    private val repository: StudyingRepository,
    private val currentTimeMs: () -> Long
) {
    @Inject
    constructor(repository: StudyingRepository) : this(repository, { System.currentTimeMillis() })

    operator fun invoke(tag: String): Flow<List<StudyingUser>> =
        combine(repository.observeStudyingUser(tag), refilterTicks()) { users, _ ->
            filterActive(users, currentTimeMs())
        }

    private fun refilterTicks(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(REFILTER_INTERVAL_MS)
        }
    }

    companion object {
        // 화면(StudyingUserCard)에 보이는 인원
        const val MAX_DISPLAY_COUNT = 3

        // 갱신 주기(1분)의 3배 — 이슈 기준(갱신 주기 × 2 이상)에 네트워크 지연·기기 시계 차이 여유 1분을 더함
        // 갱신 주기(#615)를 바꾸면 함께 바꾼다. 서버 정리 기준(STUDYING_STALE_MS, 10분)보다 짧아야 의미가 있다
        const val STALE_THRESHOLD_MS = 3 * 60 * 1000L

        // 마지막 목록을 다시 거르는 주기 — 유령은 오래된 지 최대 이 시간 뒤에 사라진다
        const val REFILTER_INTERVAL_MS = 60 * 1000L

        /** 기준 시간 안에 갱신된 사용자만 순서를 유지한 채 앞에서 [MAX_DISPLAY_COUNT]명 */
        internal fun filterActive(users: List<StudyingUser>, nowMs: Long): List<StudyingUser> =
            users.filter { user ->
                val updatedAt = user.updatedAt ?: return@filter false
                // 기기 시계가 느려 updatedAt이 미래면 음수 → 공부 중으로 본다
                nowMs - updatedAt < STALE_THRESHOLD_MS
            }.take(MAX_DISPLAY_COUNT)
    }
}
