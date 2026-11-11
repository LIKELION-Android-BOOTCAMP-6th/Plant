package com.a32b.plant.domain.network

import kotlinx.coroutines.flow.Flow

/**
 * 기기의 인터넷 연결 상태 (#531).
 * 온라인 전용 기능(출석 등)이 요청 전에 연결을 확인하고, 화면이 버튼을 비활성처럼 표시하는 데 쓴다.
 *
 * "온라인" = 인터넷 사용이 확인된 네트워크가 있음.
 * 와이파이에 연결돼 있어도 인터넷이 안 되면(인증 전 공용 와이파이 등) 오프라인으로 본다.
 */
interface NetworkMonitor {
    /** 지금 온라인인지 */
    fun isOnline(): Boolean

    /** 연결 상태 변화. 구독하면 현재 값부터 받는다 */
    fun observeIsOnline(): Flow<Boolean>
}
