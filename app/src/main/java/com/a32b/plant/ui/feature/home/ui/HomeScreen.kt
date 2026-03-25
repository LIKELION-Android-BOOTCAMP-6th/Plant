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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor


@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory.homeViewModelFactory)
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
                .background(background),
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
                    },
                    isTestMode = true
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text("아래로 내려 화분 추가 & 확인하기",
                    style = MaterialTheme.typography.bodySmall)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.height(30.dp))
            }

            // [홈 2 영역] 하단 그리드
            val chunkedPots = potList.chunked(3)

            items(chunkedPots) { rowPots ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
//                    rowPots.forEach { pot ->
//                        GridPlantItem(
//                            pot = pot,
//                            modifier = Modifier.weight(1f),
//                            // [콜백 1] 이미지 클릭 시 -> 메인 카드 교체
//                            onImageClick = {
//                                viewModel.selectPot(pot)
//                            },
//                            // [콜백 2] 텍스트 클릭 시 -> 학습 계획창으로 이동 
                              // 학습 계획창 미 개발로 인한 오류로 주석 처리
//                            onTextClick = {
//                                // Routes.Plan이 (id, tag, name)을 받는 인자 구조인지 확인 필요
//                                navController.navigate(
//                                    Routes.Plan(pot.id, pot.tag, pot.name)
//                                )
//                            }
//                        )
//                    }

                    // 빈 칸 채우기 로직
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
                    onClick = {
                        navController.navigate(Routes.NewBornTree)
                    },
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
fun MainPlantCard(displayPot: PotInfo, onStartClick: () -> Unit,
                    isTestMode: Boolean = false // 테스트용 -> 실제는 삭제
) {
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

            // 화분 이미지
            ProfileImage(
                level = displayPot.level,
                size = 150
            )

            Spacer(modifier = Modifier.height(20.dp))


            Spacer(modifier = Modifier.height(20.dp))

            // [공부 시간] 화분이 없으면 00:00:00
            Text(
                text = TimeFormatter.formatToDigitalClock(displayPot.todayStudyingTime),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onStartClick,
                // 화분이 있거나, 테스트 모드일 때 버튼을 활성화함
                enabled = displayPot.id.isNotEmpty() || isTestMode,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C16C))
            ) {
                Text(
                    text = if (displayPot.id.isEmpty() && !isTestMode) "화분 없음" else "공부 시작",
                    style = MaterialTheme.typography.titleSmall,
                    color = background
                )
            }
//            // [버튼] 화분이 없을 때는 비활성화 처리
//            Button(
//                onClick = onStartClick,
//                enabled = displayPot.id.isNotEmpty(),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(50.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA5C16C))
//            ) {
//                Text(if (displayPot.id.isEmpty()) "화분 없음" else "공부 시작",
//                    style = MaterialTheme.typography.titleSmall,
//                    color = background
//                )
//            }
        }
    }
}
@Composable
fun GridPlantItem(
    pot: PotInfo,
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit, // 이미지 클릭
    onTextClick: () -> Unit // 텍스트 클릭 여부
) {
    // 화분 ID가 비어있지 않을 때만 실제 내용을 표시
    if (pot.id.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 화분 그림 (ProfileImage) - 메인 카드 교체용
            // ProfileImage 내부에 Box를 감싸서 clickable을 적용합니다.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp)) // 동그랗게 클릭 영역 제한
                    .clickable { onImageClick() } // [클릭 1] 이미지 클릭 시
            ) {
                ProfileImage(
                    level = pot.level,
                    size = 60
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 화분 정보 (시간, 이름) - 학습 계획창 이동용
            // 텍스트 영역 전체를 클릭할 수 있도록 Column으로 묶고 clickable을 적용합니다.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTextClick() }, // [클릭 2] 텍스트 클릭 시
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 총 공부 시간 (Medium 적용)
                Text(
                    text = TimeFormatter.formatToDigitalClock(pot.todayStudyingTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
                // 화분 이름 (Medium 적용)
                Text(
                    text = pot.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    } else {
        // 화분이 없는 빈 칸은 투명한 공간으로 둠 (그리드 정렬 유지용)
        Spacer(modifier = modifier.fillMaxWidth())
    }
}