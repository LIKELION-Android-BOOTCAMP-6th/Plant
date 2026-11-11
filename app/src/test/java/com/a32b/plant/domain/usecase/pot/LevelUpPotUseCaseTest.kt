package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.PotLevelConditions
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

class LevelUpPotUseCaseTest {

    private val hour = 60 * 60 * 1000L

    @Test
    fun sendsLevelUpWithConditionItemsWhenConditionsMet() = runBlocking {
        // LV1 → LV2: 5시간, 햇빛 3 · 물 1 · 정성 5
        val pots = FakePotRepository(pot(level = 1, time = 5 * hour))
        val useCase = createUseCase(pots, items = Item(heart = 5, sun = 3, water = 1, fertilizer = 0, nutrient = 0, box = 0))

        val result = useCase("pot-1")

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(
            listOf(LevelUpRequest("uid-1", "pot-1", 1, PotLevelConditions.getCondition(1)!!.requiredItems)),
            pots.levelUps
        )
    }

    @Test
    fun pendingWriteIsReturnedAsSuccess() = runBlocking {
        val pots = FakePotRepository(pot(level = 0, time = hour), levelUpResult = Result.Success(SaveResult.Pending))
        val useCase = createUseCase(pots, items = items(100))

        assertEquals(Result.Success(SaveResult.Pending), useCase("pot-1"))
    }

    @Test
    fun studyTimeShortTellsRemainingTimeWithoutRequest() = runBlocking {
        // 1시간 조건에 30분 30.5초 학습 → 남은 시간 29분 29.5초 → 초 단위 올림
        val pots = FakePotRepository(pot(level = 0, time = 30 * 60_000L + 30_500L))
        val useCase = createUseCase(pots, items = items(100))

        val result = useCase("pot-1")

        assertEquals(Result.Failure(AppError.Custom("레벨업까지 학습 시간이 29분 30초 남았어요.")), result)
        assertTrue(pots.levelUps.isEmpty())
    }

    @Test
    fun lackingItemsAreNamedWithoutRequest() = runBlocking {
        // LV1 → LV2: 햇빛 3 · 물 1 · 정성 5 중 햇빛·물이 부족
        val pots = FakePotRepository(pot(level = 1, time = 5 * hour))
        val useCase = createUseCase(pots, items = Item(heart = 5, sun = 2, water = 0, fertilizer = 0, nutrient = 0, box = 0))

        val result = useCase("pot-1")

        assertEquals(Result.Failure(AppError.InsufficientItems("햇빛, 물이(가) 부족합니다.")), result)
        assertTrue(pots.levelUps.isEmpty())
    }

    @Test
    fun maxLevelCompletedAndMissingPotAreRejectedWithoutRequest() = runBlocking {
        val cases = listOf(
            pot(level = 6, time = 1000 * hour) to LevelUpPotUseCase.MAX_LEVEL_MESSAGE,
            pot(level = 0, time = hour).copy(isCompleted = true) to LevelUpPotUseCase.COMPLETED_POT_MESSAGE,
            null to LevelUpPotUseCase.POT_NOT_FOUND_MESSAGE
        )
        for ((pot, message) in cases) {
            val pots = FakePotRepository(pot)
            val result = createUseCase(pots, items = items(100))("pot-1")

            assertEquals(Result.Failure(AppError.Custom(message)), result)
            assertTrue(pots.levelUps.isEmpty())
        }
    }

    @Test
    fun missingUserExpiresSessionWithoutRequest() = runBlocking {
        var sessionExpiredCalls = 0
        val pots = FakePotRepository(pot(level = 0, time = hour))
        val useCase = createUseCase(pots, user = null, onSessionExpired = { sessionExpiredCalls++ })

        val result = useCase("pot-1")

        assertTrue(result is Result.Failure && result.error is AppError.UnknownUser)
        assertEquals(1, sessionExpiredCalls)
        assertTrue(pots.levelUps.isEmpty())
    }

    private fun items(count: Int) = Item(count, count, count, count, count, count)

    private fun pot(level: Int, time: Long) = Pot.EMPTY.copy(id = "pot-1", potLevel = level, potTotalStudyingTime = time)

    private fun createUseCase(
        pots: FakePotRepository,
        items: Item = items(0),
        user: User? = User.create().copy(uid = "uid-1", item = items),
        onSessionExpired: () -> Unit = {}
    ) = LevelUpPotUseCase(
        potRepository = pots,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            FakeUserRepository(user),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = onSessionExpired()
            }
        )
    )

    private data class LevelUpRequest(val uid: String, val potId: String, val currentLevel: Int, val items: Map<ItemType, Int>)

    private class FakePotRepository(
        private val cachedPot: Pot?,
        private val levelUpResult: Result<SaveResult> = Result.Success(SaveResult.Saved)
    ) : PotRepository {
        val levelUps = mutableListOf<LevelUpRequest>()

        override suspend fun getPotPreferringCache(uid: String, potId: String): Result<Pot?> = Result.Success(cachedPot)

        override suspend fun levelUpPot(
            uid: String,
            potId: String,
            currentLevel: Int,
            items: Map<ItemType, Int>
        ): Result<SaveResult> {
            levelUps += LevelUpRequest(uid, potId, currentLevel, items)
            return levelUpResult
        }

        override fun getPots(uid: String): Flow<List<Pot>> = error("Unused")
        override fun getAvailableTags(): Flow<List<Tag>> = error("Unused")
        override suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> = error("Unused")
        override suspend fun getUserPotById(uid: String, potId: String): Pot? = error("Unused")
        override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> = error("Unused")
        override suspend fun updatePotName(uid: String, potId: String, newName: String): Result<SaveResult> =
            error("Unused")
        override suspend fun deleteEntirePot(uid: String, potId: String): Result<SaveResult> = error("Unused")
        override suspend fun completeStudyPlan(uid: String, potId: String): Result<SaveResult> = error("Unused")
        override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): Result<List<StudyLog>> =
            error("Unused")
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
