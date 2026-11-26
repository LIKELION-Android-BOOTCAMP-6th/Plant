package com.a32b.plant.core.navigation

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * 맨 위 화면이 RESUMED일 때만 뒤로 간다 (#672). 상단바 뒤로가기·"돌아가기"처럼 사용자가 누르는 버튼에 쓴다.
 *
 * - 연타: 첫 pop 뒤 맨 위가 된 이전 화면은 전환이 끝날 때까지 STARTED라 두 번째 pop은 무시된다
 * - 전환 중(새 화면이 들어오는 중)에 누른 뒤로가기도 무시된다
 * - ViewModel 결과로 닫는 경우(NavigateBack 이벤트, 저장 완료 콜백)에는 쓰지 않는다.
 *   앱이 백그라운드에 있으면 화면이 RESUMED가 아니어서, 처리가 끝나도 화면이 닫히지 않기 때문이다.
 *   그런 경우는 이벤트가 한 번만 오도록 ViewModel에서 막는다.
 *
 * @return 실제로 pop했으면 true
 */
fun NavController.popBackStackSafely(): Boolean {
    val current = currentBackStackEntry ?: return false
    if (current.lifecycle.currentState != Lifecycle.State.RESUMED) return false
    return popBackStack()
}
