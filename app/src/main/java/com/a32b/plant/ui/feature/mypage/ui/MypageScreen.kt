package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.sub_green1
import com.a32b.plant.R
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1

@Composable
fun MypageScreen(navController: NavController) {
    val viewModel: MyPageViewModel = viewModel()
//    val potId by viewModel.potId.collectAsState()

    val userName by viewModel.userName.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // 프로필, 닉네임, 총 공부시간
        ProfileRow(userName, viewModel = viewModel)
//        -------
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
            DarkModeToggleButton()
        }
    }
}

@Composable
fun DarkModeToggleButton() {
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
fun GrownTreeCountButton(){
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
fun ProfileRow(userName: String, viewModel: MyPageViewModel) {
    var isOpenDialog by remember { mutableStateOf(false) }
    val isUpdateSuccess by viewModel.isUpdateSuccess.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 03-24 공통 컴포넌트 가이드 참조 fun ProfileImage() 사용하기
        Box( // 프로필이미지
            modifier = Modifier
                .size(80.dp)
                .background(Color.LightGray, shape = CircleShape)
                .clickable {
                    isOpenDialog = true
                }
        ) {

        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1F)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                Text(text = "$userName 님", style = Typography.bodySmall, color = fontColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
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
        ProfileDialog(onDismiss = { isOpenDialog = false }, userName, viewModel)
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

//@Composable
//fun UpdateProfileButton(onClick: () -> Unit) {
//    val context = LocalContext.current
//    Button(onClick = {
//        onClick()
//        Toast.makeText(context, "업데이트 완료!", Toast.LENGTH_SHORT).show()
//    })
//    {
//        Text("저장", style = Typography.bodySmall, color = fontColor)
//    }
//}


// 프로필 편집 클릭 시 이미지 3개 띄우는 함수
@Composable
fun SetImages(selectedImageLevel: String, onImageClick: (String) -> Unit) {
    Row() {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = if (selectedImageLevel == "lv.0") 3.dp else 0.dp,
                    color = sub_green1
                )
                .clickable {
                    onImageClick("lv.0")
                }
        ) {
            ProfileImage("lv.0", 30)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = if (selectedImageLevel == "lv.1") 3.dp else 0.dp,
                    color = sub_green1
                )
                .clickable {
                    onImageClick("lv.1")
                }
        ) {
            ProfileImage("lv.1", 30)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(
                    width = if (selectedImageLevel == "lv.2") 3.dp else 0.dp,
                    color = sub_green1
                )
                .clickable {
                    onImageClick("lv.2")
                }
        ) {
            ProfileImage("lv.2", 30)
        }
    }
}


@Composable
fun ProfileDialog(onDismiss: () -> Unit, userName: String, viewModel: MyPageViewModel) {
    var newUserName by remember { mutableStateOf(userName) }
    var selectedImageLevel by remember { mutableStateOf("lv.1") }
    val context = LocalContext.current
    val isUpdateSuccess by viewModel.isUpdateSuccess.collectAsState()
    LaunchedEffect(isUpdateSuccess) {
        if (isUpdateSuccess) {
            Toast.makeText(context, "업데이트 완료", Toast.LENGTH_SHORT).show()
            onDismiss()
            viewModel.resetIsUpdateSuccess()
        }
    }
    Dialog(onDismissRequest = { onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            TextField(
//                state = usernameState,
                //                ,lineLimits = TextFieldLineLimits.SingleLine,
                value = newUserName,
                onValueChange = { v ->
                    if (v.length <= 10) newUserName = v
                },
                label = {
                    Text(
                        "0글자~10글자 특수문자 제외",
                        style = Typography.bodySmall,
                        color = fontColor
                    )
                },
                placeholder = {
                    Text(
                        "변경하실 닉네임을 입력해주세요",
                        style = Typography.bodySmall,
                        color = fontColor
                    )
                }
            )
            SetImages(
                selectedImageLevel = selectedImageLevel,
                onImageClick = { clickedLevel -> selectedImageLevel = clickedLevel })
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("취소", style = Typography.bodySmall, color = fontColor)
                }
                Button(
                    onClick = {
                        if (newUserName.length <= 2) {
                            Toast.makeText(context, "닉네임 길이 3~10글자", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateProfile(newUserName, selectedImageLevel)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("저장", style = Typography.bodySmall, color = fontColor)
                }
            }
        }
    }
}

