package com.a32b.plant.ui.feature.mypage.ui

import android.R.attr.onClick
import android.R.attr.text
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel

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
            ButtonTemplate(text = "커뮤니티 활동") { }
            ButtonTemplate(text = "앱 설정") { }
            ButtonTemplate(text = "공지사항") { }
            ButtonTemplate(text = "비밀번호 재설정") { }
            ButtonTemplate(text = "다크모드-모양 변경") { }
        }
        Button(onClick = { navController.popBackStack() }) {
            Text("뒤로가기")
        }
        Text(text = "$potId 팟아이디")
    }

}

@Composable
fun ButtonTemplate(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color.Black
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
                Text(text = "$userName 님")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "총 공부 시간")
                Text(text = "20:43:30")
            }
        }
    }

    if (isOpenDialog) {
        ProfileDialog(onDismiss = { isOpenDialog = false }, userName, viewModel)
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
        Text("저장")
    }
}

//Log.d("mypage", "MypageScreen - ")
// 다이얼로그 안에 원형 이미지,
// 선택 시 테두리 강조 표시
// 1가지만 선택 가능
@Composable
fun SetImages(selectedImageLevel: String, onImageClick: (String) -> Unit) {
    Row() {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(width = if (selectedImageLevel == "lv.0") 3.dp else 0.dp,
                    color = Color.Red)
                .clickable {
                    onImageClick("lv.0")
                }
        ) {
            ProfileImage("lv.0", 30)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(width = if (selectedImageLevel == "lv.1") 3.dp else 0.dp,
                    color = Color.Red)
                .clickable {
                    onImageClick("lv.1")
                }
        ) {
            ProfileImage("lv.1", 30)
        }
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(width = if (selectedImageLevel == "lv.2") 3.dp else 0.dp,
                    color = Color.Red)
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
                value = newUserName,
                onValueChange = { v -> newUserName = v },
                label = { Text("0글자~10글자") },
                placeholder = { Text("변경하실 닉네임을 입력해주세요") }
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
                    Text("취소")
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
                    Text("저장0")
                }
            }
        }
    }
}
