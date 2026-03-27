package com.a32b.plant.core.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.theme.sub1

@Composable
fun BottomBar(navController: NavController){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Surface(modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = sub1) {
        Row(modifier = Modifier.fillMaxWidth().height(68.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {

            //커뮤니티인지 확인 하고 맞으면 커뮤니티 셀렉티드 이미지로 변경
            val isCommunitySelected = currentDestination?.hasRoute<Routes.CommunityList>() == true
            Image(
                painter = painterResource(id = if(isCommunitySelected) R.drawable.ic_bottom_community_selected else R.drawable.ic_bottom_community_normal),
                contentDescription = "COMMUNITY",
                modifier = Modifier
                    .weight(1f)
                    .size(32.dp)
                    .clickable{navController.navigate(Routes.CommunityList)}
            )

            val isHomeSelected = currentDestination?.hasRoute<Routes.HomeMain>() == true
            Image(
                painter = painterResource(id = if(isHomeSelected) R.drawable.ic_bottom_home_selected else R.drawable.ic_bottom_home_normal),
                contentDescription = "HOME",
                modifier = Modifier
                    .weight(1f)
                    .width(24.dp)
                    .height(32.dp)
                    .clickable{navController.navigate(Routes.HomeMain)}
            )

            val isMypageSelected = currentDestination?.hasRoute<Routes.Mypage>() == true
            Image(
                painter = painterResource(id = if(isMypageSelected) R.drawable.ic_bottom_mypage_selected else R.drawable.ic_bottom_mypage_normal),
                contentDescription = "MYPAGE",
                modifier = Modifier
                    .weight(1f)
                    .width(24.dp)
                    .height(32.dp)
                    .clickable{navController.navigate(Routes.Mypage)}
            )
        }
    }
}