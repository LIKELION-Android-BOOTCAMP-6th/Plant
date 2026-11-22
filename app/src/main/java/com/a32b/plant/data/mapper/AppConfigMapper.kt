package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.AppConfigDto
import com.a32b.plant.domain.model.AppVersionPolicy

/**
 * `config/app` → [AppVersionPolicy] (#662)
 * - minVersionCode가 정수가 아니면 null (확인하지 않음 → 잘못된 설정으로 모든 사용자가 막히지 않게)
 * - message가 문자열이 아니거나 비어 있으면 null (화면의 기본 문구 사용)
 */
fun AppConfigDto.toDomain(): AppVersionPolicy = AppVersionPolicy(
    minVersionCode = minVersionCode.toVersionCodeOrNull(),
    message = (message as? String)?.trim()?.takeIf { it.isNotEmpty() }
)

/** minVersionCode 필드는 있는데 정수로 읽을 수 없는지 (로그를 남길 대상) */
fun AppConfigDto.hasInvalidMinVersionCode(): Boolean =
    minVersionCode != null && minVersionCode.toVersionCodeOrNull() == null

// Firestore는 정수를 Long, 소수를 Double로 돌려준다.
// 콘솔에서 숫자 타입에 3.0처럼 넣으면 Double로 저장될 수 있어, 소수점 아래가 0인 값은 정수로 받는다
private fun Any?.toVersionCodeOrNull(): Long? = when (this) {
    is Long -> this
    is Int -> toLong()
    is Double -> if (!isNaN() && !isInfinite() && this % 1.0 == 0.0) toLong() else null
    else -> null
}
