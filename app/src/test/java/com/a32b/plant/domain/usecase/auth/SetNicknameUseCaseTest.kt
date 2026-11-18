package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.util.NicknameRule
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SetNicknameUseCaseTest {

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
    fun `invalid length is rejected without saving`() = runTest {
        val users = FakeUserRepository()
        val useCase = SetNicknameUseCase(users)

        // 1글자 / 11글자 / 공백만(정규화하면 빈 값)
        listOf("a", "가나다라마바사아자차카", "   ").forEach { input ->
            val result = useCase("uid-1", input)

            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            assertTrue(error is AppError.NicknameInvalid)
            assertEquals(NicknameRule.LENGTH_MESSAGE, error.message)
        }
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `unusable nickname is rejected without saving`() = runTest {
        val users = FakeUserRepository()
        val useCase = SetNicknameUseCase(users)

        // `/` 포함, 예약된 문서 ID 형식, 보이지 않는 문자(U+200B)
        listOf("새/싹", "__ab__", "새​싹").forEach { input ->
            val result = useCase("uid-1", input)

            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            assertTrue(error is AppError.NicknameInvalid)
            assertEquals(NicknameRule.UNUSABLE_MESSAGE, error.message)
        }
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `valid nickname is saved normalized and returned`() = runTest {
        val users = FakeUserRepository(initialUser = User.create().copy(uid = "uid-1", profileImg = "2"))
        val useCase = SetNicknameUseCase(users)

        // 앞뒤 공백 + 자모로 나뉜 "새싹" (U+1109 U+1162 U+110A U+1161 U+11A8)
        val result = useCase("uid-1", "  새싹  ")

        assertEquals(listOf("uid-1" to "새싹"), users.completedNicknames)
        assertTrue(result is Result.Success)
        assertEquals("새싹", (result as Result.Success).data)
        assertEquals("uid-1", CurrentUser.uid)
        assertEquals("새싹", CurrentUser.nickname)
        assertEquals("2", CurrentUser.profileImg)
    }

    @Test
    fun `nickname taken failure is returned as is`() = runTest {
        val users = FakeUserRepository(completeFirstLoginResult = Result.Failure(AppError.NicknameTaken()))
        val useCase = SetNicknameUseCase(users)

        val result = useCase("uid-1", "새싹")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NicknameTaken)
        assertEquals(1, users.completedNicknames.size)
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `network failure is returned as is`() = runTest {
        val users = FakeUserRepository(completeFirstLoginResult = Result.Failure(AppError.Network()))
        val useCase = SetNicknameUseCase(users)

        val result = useCase("uid-1", "새싹")

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Network)
        assertEquals(1, users.completedNicknames.size)
        assertNull(CurrentUser.user.value)
    }
}
