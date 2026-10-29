package com.a32b.plant.domain.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCooldownManager @Inject constructor() {

    // 같은 기기에서 계정을 바꿔 로그인해도 이전 계정의 쿨다운이 적용되지 않도록 uid별로 저장
    private val lastPostTimeMillisByUid = mutableMapOf<String, Long>()

    fun canPost(uid: String): Boolean =
        System.currentTimeMillis() - lastPostTimeMillis(uid) >= COOLDOWN_MILLIS

    fun getRemainingSeconds(uid: String): Int {
        val remaining = COOLDOWN_MILLIS - (System.currentTimeMillis() - lastPostTimeMillis(uid))
        return if (remaining > 0) ((remaining + 999) / 1000).toInt() else 0
    }

    fun recordPost(uid: String) {
        lastPostTimeMillisByUid[uid] = System.currentTimeMillis()
    }

    private fun lastPostTimeMillis(uid: String): Long = lastPostTimeMillisByUid[uid] ?: 0L

    companion object {
        private const val COOLDOWN_MILLIS = 30_000L
    }
}
