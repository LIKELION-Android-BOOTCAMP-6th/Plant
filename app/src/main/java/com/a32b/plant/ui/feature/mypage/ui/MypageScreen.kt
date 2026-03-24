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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.sub_green1
import com.a32b.plant.R

@Composable
fun MypageScreen(navController: NavController) {
    val viewModel: MyPageViewModel = viewModel()
    val potId by viewModel.potId.collectAsState()

    val userName by viewModel.userName.collectAsState()
    val context = LocalContext.current

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
//            --------------
            // 동작 확인 후 Dialog 에 넣기
//            --------------

            ButtonTemplate(text = "기른 나무 수-모양 변경") { }
            dividerImage()
            ButtonTemplate(text = "커뮤니티 활동") { }
            ButtonTemplate(text = "앱 설정") { }
            ButtonTemplate(text = "공지사항") { }
            ButtonTemplate(text = "비밀번호 재설정") { }
            ButtonTemplate(text = "다크모드-모양 변경") { }
        }
        Button(onClick = { navController.popBackStack() }) {
            Text("뒤로가기", style = Typography.bodySmall, color = fontColor)
        }
        Text(text = "$potId 팟아이디", style = Typography.bodySmall, color = fontColor)
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
                // 닉네임 변경 기능 가라로 완료 후 클릭 시 Dialog 띄우기
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
fun dividerImage() {
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
fun UpdateProfileButton(onClick: () -> Unit) {
    val context = LocalContext.current
    Button(onClick = {
        onClick()
        Toast.makeText(context, "업데이트 완료!", Toast.LENGTH_SHORT).show()
    })
    {
        Text("저장", style = Typography.bodySmall, color = fontColor)
    }
}


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
    var selectedImageLevel by remember { mutableStateOf("lv.0") }
    val context = LocalContext.current

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(modifier = Modifier.fillMaxWidth()) {
            TextField(
//                ------------------ 이거 어떻게 쓰는건지 알아보기
//                state = usernameState,
                //                ,lineLimits = TextFieldLineLimits.SingleLine,
                value = newUserName,

                onValueChange = { v -> newUserName = v },
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
                        onDismiss()
                        viewModel.updateProfile(newUserName, selectedImageLevel)
                        Log.d("mypage", "MyPageScreen -  $selectedImageLevel")
                        Toast.makeText(context, "업데이트 완료!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("저장", style = Typography.bodySmall, color = fontColor)
                }
            }
        }
    }
}
