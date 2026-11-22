package com.a32b.plant.domain.model

/**
 * 앱 최소 지원 버전 설정 (#662)
 *
 * @property minVersionCode 이보다 낮은 versionCode는 업데이트해야 쓸 수 있다. 값이 없거나 잘못되면 null(확인하지 않음)
 * @property message 업데이트 안내 문구. 없으면 null(화면의 기본 문구 사용)
 */
data class AppVersionPolicy(
    val minVersionCode: Long?,
    val message: String?
)
