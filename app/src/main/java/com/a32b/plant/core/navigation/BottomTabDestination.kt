package com.a32b.plant.core.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * 하단바가 보이는 탭 화면(홈·내 화분·커뮤니티·리포트·마이페이지)인지 판별한다.
 * 하단바 표시 여부(MainActivity)와 탭 ↔ 탭 페이드 전환(Navigation.kt, #674)이 같은 기준을 쓴다.
 */
fun NavDestination.isBottomTab(): Boolean =
    hasRoute<Routes.HomeMain>() ||
            hasRoute<Routes.PotList>() ||
            hasRoute<Routes.CommunityList>() ||
            hasRoute<Routes.Report>() ||
            hasRoute<Routes.Mypage>()
