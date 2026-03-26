package com.a32b.plant.ui.feature.community.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.core.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostScreen(navController: NavController) {
    val context = LocalContext.current

    // --- 상태 관리 (입력값들) ---
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("고등학생") }
    val tags = listOf("중학생", "고등학생", "대학생", "자격증", "공유")

    Scaffold(
        containerColor = Color(0xFFFDFDF0), // 연한 아이보리 배경
        topBar = {
            TopAppBar(
                title = { Text("글쓰기", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // ✅ 사각형 라운드 뒤로가기 버튼
                    Surface(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(36.dp)
                            .clickable { navController.popBackStack() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFF9575CD))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF9575CD)
                            )
                        }
                    }
                },
                actions = {
                    // ✅ 등록 버튼: 클릭 시 커뮤니티 목록으로 이동
                    Surface(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
                                } else if (content.isBlank()) {
                                    Toast.makeText(context, "본문을 입력해주세요", Toast.LENGTH_SHORT).show()
                                } else {
                                    // 1. 성공 메시지
                                    Toast.makeText(context, "성공적으로 등록되었습니다!", Toast.LENGTH_SHORT).show()

                                    // 2. 🚀 커뮤니티 목록 화면으로 이동
                                    // 주의: Routes.Community 부분은 본인의 Navigation 설정에 맞게 수정하세요.
                                    navController.navigate(Routes.CommunityDetail) {
                                        // 글쓰기 화면을 스택에서 제거 (뒤로가기 시 다시 글쓰기창이 안 나오게 함)
                                        popUpTo(Routes.CommunityPost) { inclusive = true }
                                    }
                                }
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFC5E1A5)
                    ) {
                        Text(
                            text = "등록",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF33691E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // 1️⃣ 제목 입력창
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("제목을 입력하세요", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFFC5E1A5)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 2️⃣ 태그 선택 영역
                Text(text = "카테고리 선택", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        Surface(
                            modifier = Modifier.clickable { selectedTag = tag },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF88AB75) else Color.White,
                            border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Text(
                                text = tag,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3️⃣ 본문 입력창
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = "※ 커뮤니티 규칙에 위배되는 게시글은 통보 없이 삭제될 수 있습니다.\n\n" +
                                    "1. 타인을 비방하거나 욕설 금지\n" +
                                    "2. 학업 공유 외 목적 사용 금지\n" +
                                    "3. 도배 및 광고 금지",
                            fontSize = 13.sp,
                            lineHeight = 22.sp,
                            color = Color.LightGray
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFFC5E1A5)
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}