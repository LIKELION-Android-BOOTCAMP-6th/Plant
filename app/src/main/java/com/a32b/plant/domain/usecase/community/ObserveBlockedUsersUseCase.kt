package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.repository.BlockRepository
import com.a32b.plant.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 지금 로그인한 사용자의 차단 목록 구독 (#660)
 *
 * 화면이 떠 있는 동안 로그아웃·계정 전환이 일어나도 그 시점의 사용자 목록으로 바뀌도록
 * 로그인 상태를 따라가며 구독한다. 로그인하지 않은 상태면 빈 목록.
 * 세션 만료 처리는 하지 않는다 — 구독만으로 만료 이벤트를 보내면 화면마다 중복으로 뜬다 (쓰기는 [BlockUserUseCase]가 확인)
 */
class ObserveBlockedUsersUseCase @Inject constructor(
    private val blockRepository: BlockRepository,
    private val userRepository: UserRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<BlockedUser>> =
        userRepository.currentUser
            .map { it?.uid }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(emptyList()) else blockRepository.observeBlockedUsers(uid)
            }
}
