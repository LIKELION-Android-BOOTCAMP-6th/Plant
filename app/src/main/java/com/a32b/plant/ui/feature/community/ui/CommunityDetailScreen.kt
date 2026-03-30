package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
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
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    onBack: () -> Unit,
    viewModel: CommunityDetailViewModel,
    navController: NavController
) {

    val postState by viewModel.post.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val commentText by viewModel.commentText.collectAsStateWithLifecycle()
    val isLikeProcessing by viewModel.isLikeProcessing.collectAsStateWithLifecycle()

    val showDeleteDialog = viewModel.showDeleteDialog.value

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteDialog() },
            title = { Text("게시글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("정말로 이 게시글을 삭제하시겠습니까?\n삭제된 내용은 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePost { onBack() }
                        viewModel.closeDeleteDialog()
                    }
                ) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                    Text("취소")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
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
                        Text(currentPost.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE0E0E0)))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(currentPost.nickName, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(currentPost.createdAt, color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    item {
                        Text(currentPost.content, fontSize = 16.sp, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(30.dp))
                    }

                    item {
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_community_comment), "댓글", tint = Color(0xFF6750A4), modifier = Modifier.size(18.dp))
                            Text(" ${currentPost.comments.size}", modifier = Modifier.padding(end = 12.dp))
                            
                            val isLiked = currentPost.likedBy.contains(currentUser?.uid)
                            val isAuthor = currentPost.authorUid == currentUser?.uid

                            IconButton(
                                onClick = { if (!isAuthor && !isLikeProcessing) viewModel.toggleLike() },
                                enabled = !isAuthor && !isLikeProcessing
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "좋아요",
                                    tint = if (isLiked) Color.Red else Color(0xFF6750A4),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(" ${currentPost.likeCount}")

                            Spacer(modifier = Modifier.weight(1f))


                            if (currentPost.nickName == currentUser?.nickname) {
                                IconButton(onClick = { 

                                    navController.navigate(Routes.CommunityPost(postId = currentPost.id))
                                }) {
                                    Icon(painterResource(id = R.drawable.ic_edit), "수정", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { viewModel.openDeleteDialog() }) {
                                    Icon(Icons.Default.Delete, "삭제", modifier = Modifier.size(20.dp))
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
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFFEEEEEE)))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(content, fontSize = 14.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun CommentInputSection(nickname: String, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    val isEnabled = text.isNotBlank()
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color.LightGray))
                Spacer(modifier = Modifier.width(8.dp))
                Text(nickname, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp),
                placeholder = { Text("댓글 입력...", fontSize = 13.sp) },
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
                    enabled = isEnabled,
                    modifier = Modifier.height(32.dp).width(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) Color(0xFF8BC34A) else Color.LightGray
                    ),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("등록", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}