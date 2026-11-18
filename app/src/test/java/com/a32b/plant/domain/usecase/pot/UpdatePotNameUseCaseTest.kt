package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakePotRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePotNameUseCaseTest {

    @Test
    fun savesTrimmedNameWithCurrentUserUid() = runBlocking {
        val pots = FakePotRepository()
        val useCase = createUseCase(pots)

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "  나무  ")

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(listOf(Triple("uid-1", "pot-1", "나무")), pots.updates)
    }

    @Test
    fun keepsInnerWhitespace() = runBlocking {
        val pots = FakePotRepository()
        val useCase = createUseCase(pots)

        useCase(potId = "pot-1", currentName = "새싹", newName = " 큰  나무 ")

        assertEquals("큰  나무", pots.updates.single().third)
    }

    // 오프라인 대기 결과는 그대로 전달한다 (ViewModel이 "연결되면 반영" 안내)
    @Test
    fun passesPendingResultThrough() = runBlocking {
        val pots = FakePotRepository(updatePotNameResult = Result.Success(SaveResult.Pending))
        val useCase = createUseCase(pots)

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "나무")

        assertEquals(Result.Success(SaveResult.Pending), result)
    }

    @Test
    fun passesRepositoryFailureThrough() = runBlocking {
        val pots = FakePotRepository(updatePotNameResult = Result.Failure(AppError.Permission()))
        val useCase = createUseCase(pots)

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "나무")

        assertEquals(Result.Failure(AppError.Permission()), result)
    }

    // 공백 제거 후 기존 이름과 같으면 대기열에 불필요한 쓰기를 남기지 않는다
    @Test
    fun sameNameAfterTrimSkipsWrite() = runBlocking {
        val pots = FakePotRepository()
        val useCase = createUseCase(pots)

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "  새싹 ")

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertTrue(pots.updates.isEmpty())
    }

    @Test
    fun blankNameSkipsWrite() = runBlocking {
        val pots = FakePotRepository()
        val useCase = createUseCase(pots)

        useCase(potId = "pot-1", currentName = "새싹", newName = "   ")

        assertTrue(pots.updates.isEmpty())
    }

    @Test
    fun missingUserExpiresSessionWithoutWrite() = runBlocking {
        val pots = FakePotRepository()
        var sessionExpiredCalls = 0
        val useCase = createUseCase(pots, user = null) { sessionExpiredCalls++ }

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "나무")

        assertTrue(result is Result.Failure && result.error is AppError.UnknownUser)
        assertEquals(1, sessionExpiredCalls)
        assertTrue(pots.updates.isEmpty())
    }

    private fun createUseCase(
        pots: FakePotRepository,
        user: User? = User.create().copy(uid = "uid-1"),
        onSessionExpired: () -> Unit = {}
    ) = UpdatePotNameUseCase(
        potRepository = pots,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            FakeUserRepository(user),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = onSessionExpired()
            }
        )
    )
}
