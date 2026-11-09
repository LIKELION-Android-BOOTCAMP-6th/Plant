package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

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
        val pots = FakePotRepository(result = Result.Success(SaveResult.Pending))
        val useCase = createUseCase(pots)

        val result = useCase(potId = "pot-1", currentName = "새싹", newName = "나무")

        assertEquals(Result.Success(SaveResult.Pending), result)
    }

    @Test
    fun passesRepositoryFailureThrough() = runBlocking {
        val pots = FakePotRepository(result = Result.Failure(AppError.Permission()))
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

    private class FakePotRepository(
        private val result: Result<SaveResult> = Result.Success(SaveResult.Saved)
    ) : PotRepository {
        val updates = mutableListOf<Triple<String, String, String>>()

        override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult> {
            updates += Triple(uid, potId, newName)
            return result
        }

        override fun getPots(uid: String): Flow<List<Pot>> = error("Unused")
        override fun getAvailableTags(): Flow<List<Tag>> = error("Unused")
        override suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> = error("Unused")
        override suspend fun getUserPotById(uid: String, potId: String): Pot? = error("Unused")
        override suspend fun getDuplicationLevelList(uid: String): List<Int> = error("Unused")
        override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> = error("Unused")
        override suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult> = error("Unused")
        override suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult> = error("Unused")
        override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> =
            error("Unused")
        override suspend fun levelUpWithItemConsumption(
            uid: String,
            potId: String,
            newLevel: Int,
            items: Map<ItemType, Int>
        ): Result<Unit> = error("Unused")
    }

    private class FakeUserRepository(user: User?) : UserRepository {
        override val currentUser = MutableStateFlow(user)
        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User): Unit = error("Unused")
        override fun endUserSession(): Unit = error("Unused")
        override fun takeExpiredSessionReason(): SessionExpiredReason? = error("Unused")
        override suspend fun updateLastSelectedPot(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }
}
