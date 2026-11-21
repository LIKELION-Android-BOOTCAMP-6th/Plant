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
    fun `missing required consent is rejected without saving (#661)`() = runTest {
        val users = FakeUserRepository(initialUser = User.create().copy(uid = "uid-1"))
        val useCase = SetNicknameUseCase(users)

        // 올바른 닉네임 / 형식 오류 닉네임 모두 동의 확인이 먼저다
        listOf("새싹", "a").forEach { input ->
            val result = useCase("uid-1", input, hasAgreedToRequiredTerms = false)

            assertEquals(Result.Failure(AppError.RequiredConsentMissing()), result)
        }
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `invalid length is rejected without saving`() = runTest {
        val users = FakeUserRepository()
        val useCase = SetNicknameUseCase(users)

        // 1글자 / 11글자 / 공백만(정규화하면 빈 값)
        listOf("a", "가나다라마바사아자차카", "   ").forEach { input ->
            val result = useCase("uid-1", input, hasAgreedToRequiredTerms = true)

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
            val result = useCase("uid-1", input, hasAgreedToRequiredTerms = true)

            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            assertTrue(error is AppError.NicknameInvalid)
            assertEquals(NicknameRule.UNUSABLE_MESSAGE, error.message)
        }
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `nickname with banned word is rejected without saving`() = runTest {
        val users = FakeUserRepository()
        val useCase = SetNicknameUseCase(users)

        // 금칙어 그대로 / 공백을 끼운 금칙어 / 대소문자를 섞은 금칙어
        listOf("씨발", "씨 발", "FuCk").forEach { input ->
            val result = useCase("uid-1", input, hasAgreedToRequiredTerms = true)

            assertTrue(result is Result.Failure)
            val error = (result as Result.Failure).error
            assertTrue(error is AppError.NicknameInvalid)
            assertEquals(NicknameRule.BANNED_WORD_MESSAGE, error.message)
        }
        assertTrue(users.completedNicknames.isEmpty())
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `valid nickname is saved normalized and returned`() = runTest {
        val users = FakeUserRepository(initialUser = User.create().copy(uid = "uid-1", profileImg = "2"))
        val useCase = SetNicknameUseCase(users)

        // 앞뒤 공백 + 자모로 나뉜 "새싹" (U+1109 U+1162 U+110A U+1161 U+11A8)
        val result = useCase("uid-1", "  새싹  ", hasAgreedToRequiredTerms = true)

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

        val result = useCase("uid-1", "새싹", hasAgreedToRequiredTerms = true)

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.NicknameTaken)
        assertEquals(1, users.completedNicknames.size)
        assertNull(CurrentUser.user.value)
    }

    @Test
    fun `network failure is returned as is`() = runTest {
        val users = FakeUserRepository(completeFirstLoginResult = Result.Failure(AppError.Network()))
        val useCase = SetNicknameUseCase(users)

        val result = useCase("uid-1", "새싹", hasAgreedToRequiredTerms = true)

        assertTrue(result is Result.Failure)
        assertTrue((result as Result.Failure).error is AppError.Network)
        assertEquals(1, users.completedNicknames.size)
        assertNull(CurrentUser.user.value)
    }
}
