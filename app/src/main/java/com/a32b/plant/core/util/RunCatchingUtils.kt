package com.a32b.plant.core.util

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> safeRunCatching(block: () -> T): kotlin.Result<T> =
    try {
        kotlin.Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        kotlin.Result.failure(e)
    }