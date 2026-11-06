package com.a32b.plant.data.error

import com.a32b.plant.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FirebaseFunctionsException 자체는 Android 클래스에 의존해 JVM 테스트에서 만들 수 없으므로
 * 판단 로직인 [mapFunctionsError]를 코드 이름으로 검증한다.
 */
class FunctionsErrorMapperTest {

    private fun map(codeName: String, message: String? = codeName, causedByNetwork: Boolean = false) =
        mapFunctionsError(codeName, message, causedByNetwork)

    // 오프라인이면 SDK가 INTERNAL + IOException 원인으로 던진다
    @Test
    fun `network cause maps to Network even if code is INTERNAL`() {
        assertTrue(map("INTERNAL", causedByNetwork = true) is AppError.Network)
    }

    @Test
    fun `UNAVAILABLE and DEADLINE_EXCEEDED map to Network`() {
        assertTrue(map("UNAVAILABLE") is AppError.Network)
        assertTrue(map("DEADLINE_EXCEEDED") is AppError.Network)
    }

    @Test
    fun `UNAUTHENTICATED maps to Auth and PERMISSION_DENIED maps to Permission`() {
        assertTrue(map("UNAUTHENTICATED") is AppError.Auth)
        assertTrue(map("PERMISSION_DENIED") is AppError.Permission)
    }

    // 함수가 HttpsError에 담아 보낸 사용자용 문구는 그대로 전달
    @Test
    fun `server message is passed through as Custom`() {
        assertEquals(
            AppError.Custom("오늘은 이미 출석했어요"),
            map("FAILED_PRECONDITION", message = "오늘은 이미 출석했어요"),
        )
    }

    // 문구 없이 실패하면 메시지에 코드 이름이 들어온다 (예: 배포 지역 불일치 시 NOT_FOUND)
    @Test
    fun `blank message or code name only maps to Server`() {
        assertTrue(map("NOT_FOUND") is AppError.Server)
        assertTrue(map("INVALID_ARGUMENT", message = " ") is AppError.Server)
        assertTrue(map("ALREADY_EXISTS", message = null) is AppError.Server)
    }

    @Test
    fun `INTERNAL and other codes map to Server`() {
        assertTrue(map("INTERNAL") is AppError.Server)
        assertTrue(map("UNKNOWN") is AppError.Server)
    }

    @Test
    fun `AppError is returned as is and unrelated exception maps to Unknown`() {
        val appError = AppError.InsufficientItems()
        assertEquals(appError, appError.toFunctionsAppError())
        assertTrue(IllegalStateException().toFunctionsAppError() is AppError.Unknown)
    }
}
