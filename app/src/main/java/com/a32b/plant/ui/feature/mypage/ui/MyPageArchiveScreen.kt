package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.ui.feature.home.ui.GridPlantItem
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveViewModel
import com.a32b.plant.ui.theme.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageArchiveScreen(navController: NavController) {
    val viewModel: MyPageArchiveViewModel =
        viewModel(factory = ViewModelFactory.myPageArchiveViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()


    Scaffold(
//---------------
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${uiState.nickname}의 기른 나무 수",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    //뒤로 가기
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(
                                id = R.drawable.ic_backbtn
                            ),
                            contentDescription = "뒤로가기",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
            )
        }
//---------------

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
        ) {
            // 하단 그리드
            val chunkedPots = uiState.potList.chunked(3)
            items(chunkedPots) { rowPots ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start)
                ) {
                    rowPots.forEach { pot ->
                        GridPlantItem(
                            pot = pot,
                            modifier = Modifier.weight(1f),
                            onItemClick = {
                                if (!pot.id.isNullOrEmpty()) {
                                    navController.navigate(Routes.MyPageArchiveDetail(potId = pot.id.toString()))
                                }
                            },
                        )
                    }
                    // 빈 칸 채우기 로직
                    val emptySlots = 3 - rowPots.size
                    repeat(emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun GridPlantItem(
    pot: PotInfo,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit, // 이미지 클릭 - 영역 클릭 시 로 변경하기
) {
    Log.d("plantLog", "----- $pot")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .clickable { onItemClick() }
            .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp)) // 동그랗게 클릭 영역 제한
        ) {
            ProfileImage(
                level = pot.level,
                size = 60
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 총 공부 시간
        Text(
            text = TimeFormatter.formatToDigitalClock(pot.potTotalStudyingTime ?: 0L),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 화분 이름
        Text(
            text = pot.name ?: "이름 없음",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        // 2. 화분 정보 (시간, 이름) -
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {

        }
    }
}