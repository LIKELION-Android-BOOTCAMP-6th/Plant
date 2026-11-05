package com.a32b.plant.data.error

import com.a32b.plant.domain.error.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.functions.FirebaseFunctionsException
import java.io.IOException

/**
 * Callable Function 호출 실패를 [AppError]로 바꾸는 공통 함수 (#452).
 * Repository에서 `onFailure = { e -> Result.Failure(e.toFunctionsAppError()) }`처럼 사용한다.
 *
 * - 오프라인·연결 끊김: SDK가 `INTERNAL` 코드에 원인으로 [IOException]을 담아 던지므로
 *   코드만 보지 않고 원인도 확인해 [AppError.Network]로 바꾼다. (응답 대기 초과는 `DEADLINE_EXCEEDED`)
 * - 함수가 `HttpsError("failed-precondition", "오늘은 이미 출석했어요")`처럼 사용자용 메시지를 담아 던진 경우
 *   그 메시지를 [AppError.Custom]으로 그대로 전달한다.
 */
fun Throwable.toFunctionsAppError(): AppError = when (this) {
    is AppError -> this
    is FirebaseNetworkException -> AppError.Network()
    is FirebaseFunctionsException -> mapFunctionsError(
        codeName = code.name,
        message = message,
        causedByNetwork = cause is IOException,
    )
    else -> AppError.Unknown()
}

/**
 * 실제 판단 로직. `FirebaseFunctionsException.Code`는 내부에서 Android 클래스(SparseArray)를 써서
 * JVM 단위 테스트에서 만들 수 없으므로, 코드 이름(문자열)만 받아 테스트할 수 있게 분리했다.
 * 코드 이름은 gRPC 표준 상태 코드라 바뀌지 않는다.
 */
internal fun mapFunctionsError(codeName: String, message: String?, causedByNetwork: Boolean): AppError {
    if (causedByNetwork) return AppError.Network()
    return when (codeName) {
        "UNAVAILABLE", "DEADLINE_EXCEEDED" -> AppError.Network()
        "UNAUTHENTICATED" -> AppError.Auth()
        "PERMISSION_DENIED" -> AppError.Permission()

        // 함수에서 의도적으로 던지는 코드 → 서버가 보낸 안내 문구를 보여준다
        "INVALID_ARGUMENT",
        "FAILED_PRECONDITION",
        "ALREADY_EXISTS",
        "NOT_FOUND",
        "RESOURCE_EXHAUSTED",
        "OUT_OF_RANGE",
        "ABORTED" -> serverMessageOrNull(codeName, message)?.let { AppError.Custom(it) } ?: AppError.Server()

        // INTERNAL(함수 내부 예외), UNKNOWN, CANCELLED 등
        else -> AppError.Server()
    }
}

/**
 * 함수가 메시지 없이 실패하면 SDK는 코드 이름("NOT_FOUND", "INTERNAL" 등)을 메시지로 채운다.
 * 이런 값은 사용자에게 보여줄 문구가 아니므로 제외한다.
 * (예: 앱과 함수의 배포 지역이 다르면 "NOT_FOUND"가 온다)
 */
private fun serverMessageOrNull(codeName: String, message: String?): String? =
    message?.takeIf { it.isNotBlank() && it != codeName }
