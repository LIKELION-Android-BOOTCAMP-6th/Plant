package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.R
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostScreen(navController: NavController) {

    // ✅ 1. 데이터 클래스 내부 선언
    data class Comment(
        val authorNickname: String = "닉네임",
        val content: String = "",
        val authorProfileImg: String = ""
    )

    // --- 상태 관리 ---
    var isEditing by remember { mutableStateOf(false) }
    var postContent by remember { mutableStateOf("작성글\n어쩌고 저쩌고") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 댓글 관련 상태
    var commentInput by remember { mutableStateOf("") }
    val commentList = remember { mutableStateListOf(Comment(authorNickname = "식물박사", content = "좋은 정보 감사합니다!")) }

    val postFocusRequester = remember { FocusRequester() } // 본문 수정용
    val commentFocusRequester = remember { FocusRequester() } // 댓글 입력용
    val keyboardController = LocalSoftwareKeyboardController.current
    val db = FirebaseFirestore.getInstance()

    // 본문 수정 모드 진입 시 키보드
    LaunchedEffect(isEditing) {
        if (isEditing) {
            postFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // ✅ 삭제 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("삭제 확인") },
            text = { Text("정말로 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    navController.popBackStack()
                }) { Text("확인", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                PostDetailContent(
                    content = postContent,
                    isEditing = isEditing,
                    onContentChange = { postContent = it },
                    onEditToggle = { isEditing = !isEditing },
                    onDeleteClick = { showDeleteDialog = true },
                    focusRequester = postFocusRequester
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ✅ 댓글 입력창 섹션
            item {
                CommentInputSection(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    focusRequester = commentFocusRequester,
                    onRegisterClick = {
                        if (commentInput.isNotBlank()) {
                            val newComment = Comment(authorNickname = "나", content = commentInput)

                            // 1. 리스트에 즉시 반영
                            commentList.add(0, newComment)

                            // 2. DB 저장 로직 (Firestore)
                            // db.collection("posts").document("POST_ID").collection("comments").add(newComment)

                            // 3. 입력창 초기화 및 키보드 닫기
                            commentInput = ""
                            keyboardController?.hide()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ✅ 댓글 리스트 출력
            items(commentList) { comment ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Color.LightGray) {}
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = comment.authorNickname, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = comment.content, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PostDetailContent(
    content: String,
    isEditing: Boolean,
    onContentChange: (String) -> Unit,
    onEditToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    focusRequester: FocusRequester
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = "작성글 제목", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "닉네임", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "2026년03월16일", color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            BasicTextField(
                value = content,
                onValueChange = onContentChange,
                minLines = 5,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
                cursorBrush = SolidColor(Color.Black)
            )
        } else {
            Text(text = content, minLines = 5, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.ic_community_comment), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = " ${15}", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(painter = painterResource(id = R.drawable.ic_community_like_normal), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = " ${20}", fontSize = 14.sp)

            Spacer(modifier = Modifier.weight(1f))

            IconButton(onClick = onEditToggle, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit, contentDescription = "수정", tint = if (isEditing) Color(0xFFC5E1A5) else Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
    }
}

@Composable
fun CommentInputSection(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onRegisterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(16.dp), shape = CircleShape, color = Color.LightGray) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "닉네임", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))

            // ✅ 클릭 시 키보드가 올라오도록 텍스트 필드 구현
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { focusRequester.requestFocus() }, // 영역 클릭 시 포커스
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                        textStyle = TextStyle(fontSize = 13.sp),
                        decorationBox = { innerTextField ->
                            if (value.isEmpty()) {
                                Text(text = "댓글을 입력하세요...", color = Color.LightGray, fontSize = 13.sp)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ 등록 버튼
            Surface(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(width = 50.dp, height = 24.dp)
                    .clickable { onRegisterClick() },
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFC5E1A5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "등록", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
