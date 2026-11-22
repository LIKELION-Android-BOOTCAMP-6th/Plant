package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.presentation.theme.LocalIsDarkTheme

@Composable
fun BottomBar(navController: NavController, enabled: Boolean = true) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // 탭 5개가 폭을 똑같이 나눔 (weight 1f) — 나머지 px는 버리고 탭 폭 이하로 계산
            val labelFontSize = bottomBarLabelFontSize(tabWidthPx = constraints.maxWidth / BOTTOM_BAR_LABELS.size)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.38f),
                // 탭 이름이 모두 한 줄·같은 크기라 높이가 같지만, 높이가 달라져도 아이콘 위치가 맞도록 위쪽 정렬 (#736)
                verticalAlignment = Alignment.Top
            ) {
                val isHomeSelected = currentDestination?.hasRoute<Routes.HomeMain>() == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_bottom_home_normal,
                    label = "홈",
                    selected = isHomeSelected,
                    enabled = enabled,
                    labelFontSize = labelFontSize
                ) {
                    if (!isHomeSelected) {
                        navController.navigate(Routes.HomeMain) {
                            popUpTo(Routes.HomeMain) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                val isPotSelected = currentDestination?.hasRoute<Routes.PotList>() == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_bottom_pot_normal,
                    label = "내 화분",
                    selected = isPotSelected,
                    enabled = enabled,
                    labelFontSize = labelFontSize
                ) {
                    if (!isPotSelected) {
                        navController.navigate(Routes.PotList) {
                            popUpTo(Routes.HomeMain) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                val isCommunitySelected = currentDestination?.hasRoute<Routes.CommunityList>() == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_bottom_community_normal,
                    label = "커뮤니티",
                    selected = isCommunitySelected,
                    enabled = enabled,
                    labelFontSize = labelFontSize
                ) {
                    if (!isCommunitySelected) {
                        navController.navigate(Routes.CommunityList) {
                            popUpTo(Routes.HomeMain) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                val isReportSelected = currentDestination?.hasRoute<Routes.Report>() == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_bottom_report_normal,
                    label = "리포트",
                    selected = isReportSelected,
                    enabled = enabled,
                    labelFontSize = labelFontSize
                ) {
                    if (!isReportSelected) {
                        navController.navigate(Routes.Report) {
                            popUpTo(Routes.HomeMain) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }

                val isMyPageSelected = currentDestination?.hasRoute<Routes.Mypage>() == true
                BottomBarItem(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_bottom_mypage_normal,
                    label = "마이페이지",
                    selected = isMyPageSelected,
                    enabled = enabled,
                    labelFontSize = labelFontSize
                ) {
                    if (!isMyPageSelected) {
                        navController.navigate(Routes.Mypage) {
                            popUpTo(Routes.HomeMain) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    labelFontSize: TextUnit,
    onClick: () -> Unit
) {
    val tintColor = bottomBarItemTint(MaterialTheme.colorScheme, LocalIsDarkTheme.current, selected)
    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(21.dp),
            colorFilter = ColorFilter.tint(tintColor)
        )
        // 글자 중간에서 두 줄로 쪼개지지 않게 한 줄 고정 (#736)
        // 크기는 bottomBarLabelFontSize가 다섯 탭 이름이 모두 들어가는 크기로 정함. 최소 크기로도 안 들어가는 폭이면 말줄임
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = labelFontSize,
            color = tintColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// 하단바 탭 아이콘·글자 색 (#643)
// 선택: tertiary (라이트는 기존 sub_green2와 같은 값, 다크는 EBF7E4)
// 비선택: 라이트는 서브 폰트 색(onSurfaceVariant). 다크 하단바(surface 8E9872)는 중간 밝기라
// 밝은 서브 폰트 색(B8B8B8)은 배경 대비 약 1.5:1, 선택 색과도 약 1.8:1로 구분이 약해 화면 배경색(2D2D2D, 대비 약 4.5:1)을 쓴다
internal fun bottomBarItemTint(colorScheme: ColorScheme, isDark: Boolean, selected: Boolean): Color = when {
    selected -> colorScheme.tertiary
    isDark -> colorScheme.background
    else -> colorScheme.onSurfaceVariant
}

// 하단바 탭 이름 (BottomBar의 label과 같은 순서) — 글자 크기 계산에 씀
internal val BOTTOM_BAR_LABELS = listOf("홈", "내 화분", "커뮤니티", "리포트", "마이페이지")
private val BOTTOM_BAR_LABEL_MIN_FONT_SIZE = 6.sp
private val BOTTOM_BAR_LABEL_FONT_STEP = 0.5.sp

/**
 * 하단바 탭 이름 글자 크기 (#736)
 *
 * 다섯 탭 이름이 모두 탭 폭 안에 한 줄로 들어가는 가장 큰 크기 — 탭마다 크기가 다르면 어색해서 다섯 탭을 같은 크기로 맞춤
 * - 기본 크기(bodySmall)로 들어가면 기본 크기 그대로 (탭 이름이 들어가는 폭이면 글꼴 1.0에서는 수정 전과 같은 크기.
 *   폭이 그보다 좁은 창에서는 글꼴 1.0에서도 줄어듦)
 * - 안 들어가면 0.5sp씩 줄여 보고, 최소 크기로도 안 들어가면 최소 크기 (이때 Text는 말줄임)
 * TextAutoSize를 쓰지 않는 이유: 들어가는데도 기기 dpi에 따라 한 단계 작게 나올 수 있고(PR #734), 탭마다 크기가 따로 정해짐
 */
@Composable
private fun bottomBarLabelFontSize(tabWidthPx: Int): TextUnit {
    val style = MaterialTheme.typography.bodySmall
    val measurer = rememberTextMeasurer()
    // 글꼴 배율은 Density에 들어 있어, 기기 글꼴 크기를 바꾸면 다시 계산
    val density = LocalDensity.current
    return remember(tabWidthPx, style, density, measurer) {
        fitBottomBarLabelFontSize(measurer, style, tabWidthPx)
    }
}

private fun fitBottomBarLabelFontSize(measurer: TextMeasurer, style: TextStyle, tabWidthPx: Int): TextUnit {
    val maxSize = style.fontSize.value
    val minSize = BOTTOM_BAR_LABEL_MIN_FONT_SIZE.value
    var size = maxSize
    while (size > minSize) {
        if (labelsFit(measurer, style, size.sp, tabWidthPx)) return size.sp
        size -= BOTTOM_BAR_LABEL_FONT_STEP.value
    }
    return BOTTOM_BAR_LABEL_MIN_FONT_SIZE
}

private fun labelsFit(measurer: TextMeasurer, style: TextStyle, fontSize: TextUnit, tabWidthPx: Int): Boolean =
    BOTTOM_BAR_LABELS.all { label ->
        !measurer.measure(
            text = label,
            style = style.copy(fontSize = fontSize),
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = tabWidthPx)
        ).didOverflowWidth
    }
