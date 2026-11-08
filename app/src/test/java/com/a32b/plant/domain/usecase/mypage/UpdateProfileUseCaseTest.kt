package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.NicknameRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateProfileUseCaseTest {
    @Test
    fun `too short nickname returns NicknameInvalid without checking duplicates`() = runBlocking {
        val users = FakeUserRepository(testUser())
        val useCase = createUseCase(users)

        val error = (useCase("a", "2") as Result.Failure).error

        assertTrue(error is AppError.NicknameInvalid)
        assertEquals(NicknameRule.LENGTH_MESSAGE, error.message)
        assertEquals(0, users.isNicknameTakenCalls)
    }

    @Test
    fun `nickname containing slash returns NicknameInvalid with unusable message`() = runBlocking {
        val users = FakeUserRepository(testUser())
        val useCase = createUseCase(users)

        val error = (useCase("a/b", "2") as Result.Failure).error

        assertTrue(error is AppError.NicknameInvalid)
        assertEquals(NicknameRule.UNUSABLE_MESSAGE, error.message)
        assertEquals(0, users.isNicknameTakenCalls)
    }

    @Test
    fun `taken nickname returns NicknameTaken`() = runBlocking {
        val users = FakeUserRepository(testUser(), isNicknameTakenResult = Result.Success(true))
        val useCase = createUseCase(users)

        val error = (useCase("new-nick", "2") as Result.Failure).error

        assertTrue(error is AppError.NicknameTaken)
        assertEquals(1, users.isNicknameTakenCalls)
    }

    @Test
    fun `unchanged nickname and image returns custom no-change error without checking repository`() = runBlocking {
        val users = FakeUserRepository(testUser())
        val useCase = createUseCase(users)

        val error = (useCase("test-user", "1") as Result.Failure).error

        assertTrue(error is AppError.Custom)
        assertEquals("변경사항이 없습니다.", error.message)
        assertEquals(0, users.isNicknameTakenCalls)
    }

    @Test
    fun `image only change save failure is not a nickname input error`() = runBlocking {
        val users = FakeUserRepository(
            testUser(),
            updateProfileResult = Result.Failure(AppError.Custom("프로필 수정에 실패했습니다."))
        )
        val useCase = createUseCase(users)

        val error = (useCase("test-user", "2") as Result.Failure).error

        assertTrue(error is AppError.Custom)
        assertEquals("프로필 수정에 실패했습니다.", error.message)
        assertEquals(0, users.isNicknameTakenCalls)
    }

    @Test
    fun `nickname check network failure is returned as is`() = runBlocking {
        val users = FakeUserRepository(
            testUser(),
            isNicknameTakenResult = Result.Failure(AppError.Network())
        )
        val useCase = createUseCase(users)

        val error = (useCase("new-nick", "2") as Result.Failure).error

        assertTrue(error is AppError.Network)
        assertEquals(AppError.Network().message, error.message)
    }

    private fun createUseCase(users: FakeUserRepository) = UpdateProfileUseCase(
        userRepository = users,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            users,
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
    )

    private fun testUser() = User.create().copy(
        uid = "uid-1",
        nickname = "test-user",
        profileImg = "1",
        activeSessionToken = "session-1"
    )

    private class FakeUserRepository(
        initialUser: User?,
        private val isNicknameTakenResult: Result<Boolean> = Result.Success(false),
        private val updateProfileResult: Result<Unit> = Result.Success(Unit)
    ) : UserRepository {
        override val currentUser = MutableStateFlow(initialUser)
        var isNicknameTakenCalls = 0

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
        override suspend fun updateProfile(user: User): Result<Unit> = updateProfileResult
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> {
            isNicknameTakenCalls++
            return isNicknameTakenResult
        }
        override suspend fun registerNickname(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun deleteNickname(nickname: String): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }
}
