package com.a32b.plant.data.model

/**
 * Firestore `config/app` 문서 (#662)
 *
 * 값은 Firebase 콘솔에서 사람이 직접 넣으므로 타입이 틀릴 수 있다.
 * toObject로 읽으면 타입이 다를 때 예외가 나서 "잘못된 값"과 "읽기 실패"를 구분할 수 없으므로,
 * 필드 값을 그대로 담고 타입 확인은 매퍼(AppConfigMapper)에서 한다.
 */
data class AppConfigDto(
    var minVersionCode: Any? = null,
    var message: Any? = null
)
