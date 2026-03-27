package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.a32b.plant.R
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    onBack: () -> Unit,
    viewModel: CommunityDetailViewModel
) {
    val postState by viewModel.post.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val commentText by viewModel.commentText.collectAsStateWithLifecycle()

    var isEditing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }

    LaunchedEffect(postState) {
        postState?.let {
            editTitle = it.title
            editContent = it.content
        }
    }

    Scaffold(
        containerColor = Color(0xFFF9F9F4),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(id = R.drawable.ic_backbtn), contentDescription = "뒤로", modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        val currentPost = postState ?: return@Scaffold


        val formattedDate = remember(currentPost.createdAt) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("yyyy년MM월dd일", Locale.KOREA)
                val date = inputFormat.parse(currentPost.createdAt)
                date?.let { outputFormat.format(it) } ?: currentPost.createdAt
            } catch (e: Exception) {
                currentPost.createdAt
            }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(20.dp)) {

                    item {
                        if (isEditing) {
                            OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text(currentPost.title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }


                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.LightGray))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(currentPost.nickName, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))

                            Text(formattedDate, color = Color.Gray, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(25.dp))
                    }


                    item {
                        if (isEditing) {
                            OutlinedTextField(value = editContent, onValueChange = { editContent = it }, modifier = Modifier.fillMaxWidth().height(150.dp))
                        } else {
                            Text(currentPost.content, fontSize = 16.sp, lineHeight = 24.sp)
                        }
                        Spacer(modifier = Modifier.height(40.dp))
                    }


                    item {
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_community_comment), "댓글", tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                            Text(" ${currentPost.comments.size}", modifier = Modifier.padding(end = 12.dp))
                            Icon(Icons.Default.FavoriteBorder, "좋아요", tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                            Text(" ${currentPost.likeCount}")

                            Spacer(modifier = Modifier.weight(1f))

                            if (currentPost.nickName == currentUser?.nickname) {
                                IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(24.dp)) {
                                    Icon(painterResource(id = R.drawable.ic_edit), "수정")
                                }
                                IconButton(onClick = { viewModel.deletePost { onBack() } }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, "삭제")
                                }
                            }
                        }
                    }


                    item {
                        CommentInputSection(
                            nickname = currentUser?.nickname ?: "익명",
                            text = commentText,
                            onTextChange = { viewModel.onCommentChange(it) },
                            onSend = { viewModel.addComment() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ✅ 6. 실제 댓글 목록 (DB 데이터 연결)
                    items(currentPost.comments) { commentData ->
                        val name = commentData["nickName"] as? String ?: "익명"
                        val content = commentData["content"] as? String ?: ""

                        CommentRow(name = name, content = content)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(name: String, content: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.LightGray))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(content, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun CommentInputSection(nickname: String, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.LightGray))
                Spacer(modifier = Modifier.width(8.dp))
                Text(nickname, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().height(80.dp).padding(top = 8.dp),
                placeholder = { Text("댓글작성", fontSize = 14.sp, color = Color.LightGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Button(
                    onClick = onSend,
                    modifier = Modifier.height(30.dp).width(60.dp).padding(top = 4.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("등록", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
