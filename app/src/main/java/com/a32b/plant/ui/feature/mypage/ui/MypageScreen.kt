package com.a32b.plant.ui.feature.mypage.ui

import android.R.attr.onClick
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
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
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageEvent
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageUiState
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub2
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.ui.theme.PlantTheme
import com.a32b.plant.ui.theme.sub_green2


@Composable
fun MyPageScreen(navController: NavController) {
    val viewModel: MyPageViewModel = viewModel(factory = ViewModelFactory.myPageViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    // 로그아웃 확인 다이얼로그 상태
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    PlantTheme(darkTheme = uiState.isDarkMode) {
        // 로그아웃
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is MyPageEvent.ShowToast ->
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()

                    is MyPageEvent.NavigateToSignIn ->
                        navController.navigate(Routes.SignIn) {
                            popUpTo(0) { inclusive = true }
                        }

                    is MyPageEvent.NavigateToMyCommunityFeed ->
                        navController.navigate(Routes.MyCommunityFeed)
                }
            }
        }

        // 로그아웃 확인 다이얼로그
        if (showLogoutDialog) {
            ConfirmDialog(
                text = "로그아웃 하시겠습니까?",
                onDismiss = {
                    showLogoutDialog = false
                    viewModel.clearProfileState()
                },
                onConfirm = {
                    showLogoutDialog = false
                    viewModel.logout()
                }
            )
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 프로필, 닉네임, 총 공부시간
                ProfileRow(uiState = uiState, viewModel = viewModel)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GrownTreesButton(completedPotCount = uiState.completedPotCount) {
                        navController.navigate(Routes.MyPageArchive)
                    }
                    DividerImage()
                    ButtonTemplate(text = "내 활동") {
                        viewModel.moveToMyCommunityFeed()
                    }
                    ButtonTemplate(text = "앱 설정") {
                        navController.navigate(Routes.MyPageSetting)
                    }
//            ButtonTemplate(text = "공지사항") { }
//            ButtonTemplate(text = "비밀번호 재설정") { }
                    DarkModeToggleButton(
                        isDarkMode = uiState.isDarkMode,
                        onToggle = {
                            viewModel.toggleDarkMode()
                        }
                    )
//------------------로그아웃

                    ButtonTemplate(text = "로그아웃") {
                        showLogoutDialog = true
                    }
//로그아웃------------------
                }
            }
        }
    }
}

@Composable
fun DarkModeToggleButton(
    isDarkMode: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,

            ) {
            // 좌측
            Text(
                text = "다크모드",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
            // 우측
            Switch(
                checked = isDarkMode,
                modifier = Modifier.scale(0.9f),
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                ),
            )
        }
    }
}

@Composable
fun GrownTreesButton(
    completedPotCount: Int, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically

        ) {
            Text(
                text = "기른 나무 수",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$completedPotCount 그루",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun ButtonTemplate(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
//            containerColor = MaterialTheme.colorScheme.surface,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface)
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
        Box(
            modifier = Modifier
                .clickable {
                    viewModel.getImageLevelList()
                    isOpenDialog = true
                }
        ) {

            ProfileImage(
                level = uiState.profileImg.replace("lv.", ""),
                size = 60
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = "프로필 수정",

                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp),
                tint = MaterialTheme.colorScheme.onSurface
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
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "총 공부 시간",
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = uiState.totalStudyTime,
                    style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (isOpenDialog) {
        ProfileDialog(
            onDismiss = {
                viewModel.clearProfileState()
                isOpenDialog = false
            },
            uiState = uiState,
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
            modifier = Modifier.size(45.dp),
        )
    }
}

// 프로필 편집 다이얼로그 화분 이미지 배치
@Composable
fun SetImages(
    levelList: List<String>,
    selectedImageLevel: String,
    onImageClick: (String) -> Unit
) {
    Log.d("PlantLog", "$levelList")
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        levelList.forEach { level ->
            val removeTextResult = level.replace("lv.", "")
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selectedImageLevel == level) 5.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { onImageClick(level) }
            ) {
                ProfileImage(level = removeTextResult, size = 60)
            }
        }
    }
}


@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    uiState: MyPageUiState,
    viewModel: MyPageViewModel
) {
    // 다이얼로그 안에서만 임시로 쓸 상태들 (입력 중인 값)
    var newUserName by remember { mutableStateOf(uiState.nickname) }
    var selectedImageLevel by remember { mutableStateOf(uiState.profileImg) }
    1
    val context = LocalContext.current

    // 업데이트 성공 시 창 닫기 로직
    LaunchedEffect(uiState.isUpdateSuccess) {
        if (uiState.isUpdateSuccess) {
            Toast.makeText(context, "업데이트 완료", Toast.LENGTH_SHORT).show()
            viewModel.clearProfileState()
            onDismiss()
            viewModel.resetIsUpdateSuccess()
        }
    }

    Dialog(onDismissRequest = {
        viewModel.clearProfileState()
        onDismiss()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
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
                        onValueChange = {
                            if (it.length <= 10)
                                newUserName = it
                            viewModel.resetNicknameError()
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        label = { Text("닉네임 변경 (2~10자)", style = Typography.labelSmall) },
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
                    Button(
                        onClick = {
                            viewModel.clearProfileState()
                            onDismiss()
                        },
                        modifier = Modifier
                            .height(45.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) { Text("취소", style = MaterialTheme.typography.bodyMedium) }

                    Button(
                        onClick = {
                            viewModel.updateProfile(newUserName, selectedImageLevel)
                        },
                        enabled = !uiState.isLoading, // 작업중이면 버튼 클릭 비활성화 하려고 추가
                        modifier = Modifier
                            .height(45.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("저장", style = Typography.bodyMedium) }
                }
            }
        }
    }
}
