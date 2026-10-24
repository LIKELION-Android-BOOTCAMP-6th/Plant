package com.a32b.plant.domain.usecase.community

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCooldownManager @Inject constructor() {

    private var lastPostTimeMillis: Long = 0L

    fun canPost(): Boolean =
        System.currentTimeMillis() - lastPostTimeMillis >= COOLDOWN_MILLIS

    fun getRemainingSeconds(): Int {
        val remaining = COOLDOWN_MILLIS - (System.currentTimeMillis() - lastPostTimeMillis)
        return if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    fun recordPost() {
        lastPostTimeMillis = System.currentTimeMillis()
    }

    companion object {
        private const val COOLDOWN_MILLIS = 30_000L
    }
}
