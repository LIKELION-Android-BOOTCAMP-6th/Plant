package com.a32b.plant.domain.service

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostCooldownManager @Inject constructor() {

    // 같은 기기에서 계정을 바꿔 로그인해도 이전 계정의 쿨다운이 적용되지 않도록 uid별로 저장
    // 싱글톤이라 Map 읽기/쓰기는 스레드 안전하게 ConcurrentHashMap 사용 (확인→기록의 원자성은 isSubmitting으로 보장)
    private val lastPostTimeMillisByUid = ConcurrentHashMap<String, Long>()

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
