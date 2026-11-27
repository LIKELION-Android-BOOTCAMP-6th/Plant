package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.NicknameRule
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateProfileUseCaseTest {

    // 성공 경로는 전역 CurrentUser를 바꾸므로 테스트 전후로 비운다.
    @Before
    fun setUp() {
        CurrentUser.clear()
    }

    @After
    fun tearDown() {
        CurrentUser.clear()
    }

    @Test
    fun `missing current user returns UnknownUser without updating`() = runTest {
        val users = FakeUserRepository(initialUser = null)
        val useCase = createUseCase(users)

        val result = useCase("새싹", "2")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.UnknownUser)
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `unchanged nickname and image returns no-change error without updating`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        val useCase = createUseCase(users)

        val result = useCase("새싹", "1")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Custom)
        assertEquals("변경사항이 없어요.", error.message)
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `image only change keeps current nickname`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        users.updateProfileResult = Result.Success(Unit)
        val useCase = createUseCase(users)

        val result = useCase("새싹", "3")

        assertEquals(1, users.updatedUsers.size)
        val sent = users.updatedUsers.single()
        assertEquals("새싹", sent.nickname)
        assertEquals("3", sent.profileImg)
        assertTrue(result is Result.Success)
        assertEquals("새싹", (result as Result.Success).data)
        assertEquals("새싹", CurrentUser.nickname)
        assertEquals("3", CurrentUser.profileImg)
    }

    @Test
    fun `nickname change sends normalized nickname and returns it`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        users.updateProfileResult = Result.Success(Unit)
        val useCase = createUseCase(users)

        val result = useCase("  새로운  ", "2")

        assertEquals(1, users.updatedUsers.size)
        val sent = users.updatedUsers.single()
        assertEquals("새로운", sent.nickname)
        assertEquals("2", sent.profileImg)
        assertTrue(result is Result.Success)
        assertEquals("새로운", (result as Result.Success).data)
        assertEquals("새로운", CurrentUser.nickname)
        assertEquals("2", CurrentUser.profileImg)
    }

    @Test
    fun `invalid nickname length is rejected without updating`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        val useCase = createUseCase(users)

        val tooShort = useCase("a", "1")
        val tooLong = useCase("가나다라마바사아자차카", "1")

        listOf(tooShort, tooLong).forEach { result ->
            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            assertTrue(error is AppError.NicknameInvalid)
            assertEquals(NicknameRule.LENGTH_MESSAGE, error.message)
        }
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `repository nickname taken failure is returned as is`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        users.updateProfileResult = Result.Failure(AppError.NicknameTaken())
        val useCase = createUseCase(users)

        val result = useCase("새로운", "1")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NicknameTaken)
        assertEquals(1, users.updatedUsers.size)
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `repository network failure is returned as is`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        users.updateProfileResult = Result.Failure(AppError.Network())
        val useCase = createUseCase(users)

        val result = useCase("새로운", "1")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Network)
        assertEquals(1, users.updatedUsers.size)
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `image only change save failure is not a nickname input error`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        users.updateProfileResult = Result.Failure(AppError.Custom("프로필 저장에 실패했어요. 잠시 후 다시 시도해주세요."))
        val useCase = createUseCase(users)

        val result = useCase("새싹", "2")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Custom)
        assertEquals("프로필 저장에 실패했어요. 잠시 후 다시 시도해주세요.", error.message)
    }

    @Test
    fun `server nickname with trailing space is kept when only image changes`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹 ", profileImg = "1"))
        users.updateProfileResult = Result.Success(Unit)
        val useCase = createUseCase(users)

        val result = useCase("새싹", "3")

        assertEquals(1, users.updatedUsers.size)
        val sent = users.updatedUsers.single()
        assertEquals("새싹 ", sent.nickname)
        assertEquals("3", sent.profileImg)
        assertTrue(result is Result.Success)
        assertEquals("새싹 ", (result as Result.Success).data)
    }

    @Test
    fun `same nickname with trailing space and same image returns no-change error`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹 ", profileImg = "1"))
        val useCase = createUseCase(users)

        val result = useCase("새싹", "1")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.Custom)
        assertEquals("변경사항이 없어요.", error.message)
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `nickname with invisible character is rejected with unusable message`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        val useCase = createUseCase(users)

        // 길이는 맞지만 U+200B(폭 없는 공백)가 섞여 있어 사용할 수 없는 닉네임이다
        val result = useCase("새​싹", "1")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.NicknameInvalid)
        assertEquals(NicknameRule.UNUSABLE_MESSAGE, error.message)
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `nickname change with banned word is rejected without updating`() = runTest {
        val users = FakeUserRepository(testUser(nickname = "새싹", profileImg = "1"))
        val useCase = createUseCase(users)

        val result = useCase("씨.발", "1")

        assertTrue(result is Result.Failure)
        val error = (result as Result.Failure).error
        assertTrue(error is AppError.NicknameInvalid)
        assertEquals(NicknameRule.BANNED_WORD_MESSAGE, error.message)
        assertTrue(users.updatedUsers.isEmpty())
    }

    @Test
    fun `existing nickname with banned word is not checked when only image changes`() = runTest {
        // 금칙어 검사가 생기기 전에 등록된 닉네임 — 닉네임을 바꾸지 않으면 검사하지 않는다
        val users = FakeUserRepository(testUser(nickname = "씨발", profileImg = "1"))
        users.updateProfileResult = Result.Success(Unit)
        val useCase = createUseCase(users)

        val result = useCase("씨발", "3")

        assertTrue(result is Result.Success)
        assertEquals("씨발", (result as Result.Success).data)
        assertEquals("씨발", users.updatedUsers.single().nickname)
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

    private fun testUser(nickname: String, profileImg: String) = User.create().copy(
        uid = "uid-1",
        nickname = nickname,
        profileImg = profileImg
    )
}
