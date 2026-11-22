package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeBlockRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 사용자 차단 UseCase (#660) */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockUseCasesTest {

    private val userRepository = FakeUserRepository(User.create().copy(uid = ME))
    private var sessionExpiredCount = 0
    private val ensureCurrentUser = EnsureCurrentUserUseCase(
        userRepository,
        object : SessionExpiredNotifier {
            override fun notifySessionExpired(reason: SessionExpiredReason) { sessionExpiredCount++ }
        }
    )
    private val blockRepository = FakeBlockRepository()

    @Test
    fun `다른 사용자를 차단하면 내 uid 목록에 차단 시점 닉네임과 함께 저장한다`() = runTest {
        val result = BlockUserUseCase(blockRepository, ensureCurrentUser)("bad", "나쁜", "3")

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(listOf(ME to BlockedUser("bad", "나쁜", "3", null)), blockRepository.blockCalls)
    }

    @Test
    fun `나 자신이나 빈 uid는 저장소까지 가지 않고 실패한다`() = runTest {
        val useCase = BlockUserUseCase(blockRepository, ensureCurrentUser)

        assertEquals(Result.Failure(AppError.Custom(BlockUserUseCase.SELF_BLOCK_MESSAGE)), useCase(ME, "나", "1"))
        assertEquals(Result.Failure(AppError.Custom(BlockUserUseCase.INVALID_TARGET_MESSAGE)), useCase("", "", ""))
        assertTrue(blockRepository.blockCalls.isEmpty())
    }

    @Test
    fun `로그인 정보가 없으면 세션 만료로 처리하고 쓰지 않는다`() = runTest {
        userRepository.currentUser.value = null

        val blockResult = BlockUserUseCase(blockRepository, ensureCurrentUser)("bad", "나쁜", "3")
        val unblockResult = UnblockUserUseCase(blockRepository, ensureCurrentUser)("bad")

        assertTrue((blockResult as Result.Failure).error is AppError.UnknownUser)
        assertTrue((unblockResult as Result.Failure).error is AppError.UnknownUser)
        assertEquals(2, sessionExpiredCount)
        assertTrue(blockRepository.blockCalls.isEmpty())
        assertTrue(blockRepository.unblockCalls.isEmpty())
    }

    @Test
    fun `차단 해제는 내 uid 목록에서 뺀다`() = runTest {
        val result = UnblockUserUseCase(blockRepository, ensureCurrentUser)("bad")

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(listOf(ME to "bad"), blockRepository.unblockCalls)
    }

    @Test
    fun `로그인한 사용자를 따라 구독하고, 로그아웃하면 빈 목록, 다른 계정이면 그 계정 목록을 보낸다`() = runTest {
        blockRepository.blockedOf(ME).value = listOf(BlockedUser("bad", "나쁜", "1", 1L))
        blockRepository.blockedOf(OTHER).value = listOf(BlockedUser("x", "엑스", "1", 1L))
        val received = mutableListOf<List<String>>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveBlockedUsersUseCase(blockRepository, userRepository)().collect { list -> received += list.map { it.uid } }
        }

        // 같은 사용자의 문서 갱신(닉네임 변경 등)은 다시 구독하지 않는다
        userRepository.currentUser.value = User.create().copy(uid = ME, nickname = "바뀐 닉네임")
        userRepository.currentUser.value = null
        userRepository.currentUser.value = User.create().copy(uid = OTHER)
        job.cancel()

        assertEquals(listOf(listOf("bad"), emptyList(), listOf("x")), received)
        assertEquals(listOf(ME, OTHER), blockRepository.observedUids)
    }

    private companion object {
        const val ME = "me"
        const val OTHER = "other"
    }
}
