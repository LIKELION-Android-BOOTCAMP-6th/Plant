package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.sub_green2
import com.a32b.plant.core.navigation.Routes

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.38f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHomeSelected = currentDestination?.hasRoute<Routes.HomeMain>() == true
            BottomBarItem(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_bottom_home_normal,
                label = "홈",
                selected = isHomeSelected,
                enabled = enabled
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
                label = "화분",
                selected = isPotSelected,
                enabled = enabled
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
                enabled = enabled
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
                enabled = enabled
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
                enabled = enabled
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

@Composable
private fun BottomBarItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val tintColor = if (selected) sub_green2 else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(21.dp),
            colorFilter = ColorFilter.tint(tintColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = tintColor
        )
    }
}
