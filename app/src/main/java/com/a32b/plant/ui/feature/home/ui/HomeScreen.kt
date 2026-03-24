package com.a32b.plant.ui.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel()
    val userName by viewModel.userName.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState() // 로컬 날짜 획득
    val displayPot by viewModel.displayPot.collectAsState()
    val potList by viewModel.potList.collectAsState()

    Scaffold(
        topBar = {
            Text(
                text = "${userName}의 Garden",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.displayLarge
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF9F9F4)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // [홈 1 영역] 상단 메인 카드
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(currentDate,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(20.dp))

                // 메인 카드
                MainPlantCard(
                    displayPot = displayPot,
                    onStartClick = {
                        // 화분이 있을 때만 공부 페이지로 이동
                        if (displayPot.id.isNotEmpty()) {
                            navController.navigate(
                                Routes.Studying(displayPot.id, displayPot.tag, displayPot.name)
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("아래로 내려 나만의 화분 확인하기",
                    style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.height(30.dp))
            }

            // [홈 2 영역] 하단 그리드 (예시 데이터 사용, 필요시 mypage 컬렉션 연결)
            val chunkedPots = potList.chunked(3)

            items(chunkedPots) { rowPots ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 실제 데이터가 있는 화분들 표시
                    rowPots.forEach { pot ->
                        GridPlantItem(
                            pot = pot,
                            modifier = Modifier.weight(1f),
                            onItemClick = {
                                //클릭 시 ViewModel -> selectPot 함수 호출
                                viewModel.selectPot(pot)                            }
                        )
                    }
                    //빈 칸 채우기 로직 (rowPots.size가 3보다 작을 때)
                    val emptySlots = 3 - rowPots.size
                    repeat(emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            // [추가 버튼 영역]
            item {
                Spacer(modifier = Modifier.height(30.dp))
                IconButton(
                    onClick = { /* 화분 추가 페이지로 이동 */ },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "화분 추가",
                        modifier = Modifier.fillMaxSize(),
                        tint = Color(0xFFA5C16C)
                    )
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

@Composable
fun MainPlantCard(displayPot: PotInfo, onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(0.85f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //화분이 있을 때만 태그 표시 (ID가 비어있지 않을 때)
            if (displayPot.id.isNotEmpty() && displayPot.tag.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = displayPot.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 화분이 없으면 안내 문구 - Bold 적용
            Text(
                text = if (displayPot.id.isEmpty()) "화분을 등록해보세요" else displayPot.name,
                style = MaterialTheme.typography.displayLarge,
                color = fontColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // [이미지 영역] 화분 유무에 따른 분기
            ProfileImage(
                level = displayPot.level,
                size = 150
            )

            Spacer(modifier = Modifier.height(20.dp))


            Spacer(modifier = Modifier.height(20.dp))

            // [공부 시간] 화분이 없으면 00:00:00
            Text(
                text = TimeFormatter.formatToDigitalClock(displayPot.todayStudyingTime),                fontSize = 24.sp,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            // [버튼] 화분이 없을 때는 비활성화 처리
            Button(
                onClick = onStartClick,
                enabled = displayPot.id.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C16C))
            ) {
                Text(if (displayPot.id.isEmpty()) "화분 없음" else "공부 시작",
                    style = MaterialTheme.typography.titleSmall,
                    color = background
                )
            }
        }
    }
}
@Composable
fun GridPlantItem(
    pot: PotInfo,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit
) {
    // 화분 ID가 비어있지 않을 때만 실제 내용을 표시
    if (pot.id.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .clickable { onItemClick() }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 화분 그림 (ProfileImage 재사용)
            ProfileImage(
                level = pot.level,
                size = 60
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 총 공부 시간 (TimeFormatter 사용, Medium 적용)
            Text(
                text = TimeFormatter.formatToDigitalClock(pot.todayStudyingTime),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black // 포인트 컬러
            )

            // 3. 화분 이름 (Medium 적용)
            Text(
                text = pot.name,
                style = MaterialTheme.typography.bodySmall,maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    } else {
        // 화분이 없는 빈 칸은 투명한 공간으로 둠 (그리드 정렬 유지용)
        Spacer(modifier = modifier.fillMaxWidth())
    }
}