package com.a32b.plant.ui.feature.mypage.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.sub_green1
import com.a32b.plant.R
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageUiState
import com.a32b.plant.ui.theme.primary

@Composable
fun MypageScreen(navController: NavController) {
    val viewModel: MyPageViewModel = viewModel(factory = ViewModelFactory.myPageViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 프로필, 닉네임, 총 공부시간
        ProfileRow(uiState = uiState, viewModel = viewModel)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ButtonTemplate(text = "기른 나무 수-모양 변경") { }
            DividerImage()
            ButtonTemplate(text = "커뮤니티 활동") { }
            ButtonTemplate(text = "앱 설정") { }
            ButtonTemplate(text = "공지사항") { }
            ButtonTemplate(text = "비밀번호 재설정") { }
            DarkModeToggleButton(
                isDarkMode = uiState.isDarkMode,
                onToggle = {
                    viewModel.toggleDarkMode()
                }
            )
        }
    }
}


@Composable
fun DarkModeToggleButton(
    isDarkMode: Boolean,
    onToggle: () -> Unit
) {
    val isDark = remember { mutableStateOf(false) }

    Button(
        onClick = { isDark.value = !isDark.value },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 좌측
            Text(
                text = "다크모드",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isDark.value,
                onCheckedChange = { isDark.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primary
                ),
            )
        }
    }
}

@Composable
fun GrownTreeCountButton() {
// 임시로 정한 숫자 (나중엔 서버나 DB에서 가져오겠지?)
    val count = 5
    Button(
        onClick = { /* 클릭 시 나무 상세 페이지로 이동 등 */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽: 제목 (폰트 작게)
            Text(
                text = "기른 나무 수",
                style = MaterialTheme.typography.bodyMedium
            )

            // 오른쪽: 숫자 상태 (폰트 더 작게)
            Text(
                text = "${count}그루",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50) // 나무니까 초록색 포인트!
            )
        }
    }
}

@Composable
fun ButtonTemplate(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = fontColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ProfileRow(uiState: MyPageUiState, viewModel: MyPageViewModel) {
    var isOpenDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [수정 포인트] 03-24 공통 컴포넌트 가이드 적용
        Box(
            modifier = Modifier
                .clickable {
                    viewModel.getImageLevelList()
                    isOpenDialog = true
                }
        ) {
            ProfileImage(
                level = uiState.profileImg,
                size = 60
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1F)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${uiState.nickname} 님",
                    style = Typography.bodySmall,
                    color = fontColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "총 공부 시간", style = Typography.bodySmall, color = fontColor)
                Text(text = "20:43:30", style = Typography.bodySmall, color = fontColor)
            }
        }
    }

    if (isOpenDialog) {
        ProfileDialog(
            onDismiss = { isOpenDialog = false },
            uiState = uiState, // 뷰모델의 상태 가방 전달
            viewModel = viewModel
        )
    }
}
@Composable
fun DividerImage() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_mypage_divider),
            contentDescription = "구분선",
            modifier = Modifier.size(45.dp)
        )
    }
}

@Composable
fun SetImages(
    levelList: List<String>,
    selectedImageLevel: String,
    onImageClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        levelList.forEach { level ->
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selectedImageLevel == level) 3.dp else 0.dp,
                        color = sub_green1,
                        shape = CircleShape
                    )
                    .clickable { onImageClick(level) }
            ) {
                // 네가 요청한 형식 그대로!
                ProfileImage(level, 60)
            }
        }
    }
}


@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    uiState: MyPageUiState, // 1. 이제 가방(uiState)을 직접 받아!
    viewModel: MyPageViewModel
) {
    // 다이얼로그 안에서만 임시로 쓸 상태들 (입력 중인 값)
    var newUserName by remember { mutableStateOf(uiState.nickname) }
    var selectedImageLevel by remember { mutableStateOf(uiState.profileImg) }

    val context = LocalContext.current

    // 2. [중요] 업데이트 성공 시 창 닫기 로직
    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            Toast.makeText(context, "업데이트 완료", Toast.LENGTH_SHORT).show()
            onDismiss()
            viewModel.resetIsUpdateSuccess()
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        value = newUserName,
                        onValueChange = { if (it.length <= 10) newUserName = it },
                        label = { Text("닉네임 변경 (3~10자)", style = Typography.labelSmall) },
                        isError = uiState.nicknameError != null
                    )

                    if (uiState.nicknameError != null) {
                        Text(
                            text = uiState.nicknameError,
                            color = Color.Red,
                            style = Typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }
                }

                SetImages(
                    levelList = uiState.levelList,
                    selectedImageLevel = selectedImageLevel,
                    onImageClick = { selectedImageLevel = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onDismiss() }, modifier = Modifier.weight(1f)) {
                        Text("취소")
                    }
                    Button(
                        onClick = {
                            viewModel.updateProfile(newUserName, selectedImageLevel)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("저장")
                    }
                }
            }
        }
    }
}