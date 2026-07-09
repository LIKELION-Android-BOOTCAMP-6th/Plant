package com.a32b.plant.domain.result

import com.a32b.plant.domain.error.AppError

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val error: AppError) : Result<Nothing>()
}

/**
 * Result 객체를 편리하게 처리하기 위한 확장 함수들입니다.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onFailure(action: (AppError) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

/**
 * 데이터 변환 시 사용하는 확장 함수입니다.
 * 성공 시 데이터를 변환하며, 실패 시에는 기존 에러를 그대로 반환합니다.
 */
fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Failure -> Result.Failure(error)
    }
}