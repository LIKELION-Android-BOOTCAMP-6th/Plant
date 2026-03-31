package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
    navController: NavController
) {

    val route = navController.currentBackStackEntry?.toRoute<Routes.CommunityDetail>()
    val postId = route?.postId ?: ""


    val viewModel: CommunityDetailViewModel = viewModel(
        factory = ViewModelFactory.communityDetailViewModelFactory(postId)
    )
    val uiState by viewModel.uiState.collectAsState()

    val postState by viewModel.post.collectAsStateWithLifecycle()
    val showDeleteDialog = viewModel.showDeleteDialog.value


    val isLikedByMe = postState?.isLiked == true


    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteDialog() },
            title = { Text("게시글 삭제", color = Color.Black, fontWeight = FontWeight.Bold) },
            text = { Text("정말로 삭제하시겠습니까?", color = Color.Black) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost { navController.popBackStack() }
                    viewModel.closeDeleteDialog()
                }) {
                    Text("삭제", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                    Text("취소", color = Color.Black)
                }
            },
            containerColor = sub2
        )
    }

    Scaffold(
        containerColor = background,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painterResource(id = R.drawable.ic_backbtn),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        val currentPost = postState ?: return@Scaffold

        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 20.dp)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = sub2),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(20.dp)) {

                    item {
                        Text(currentPost.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(sub3))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(currentPost.author.nickname, fontWeight = FontWeight.Medium, color = Color.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(TimeFormatter.formatTimestamp(currentPost.createdAt), color = Color.Black, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }


                    item {
                        Text(currentPost.content, fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(30.dp))
                    }


                    item {
                        HorizontalDivider(color = sub3.copy(alpha = 0.5f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_community_comment), null, tint = primary, modifier = Modifier.size(18.dp))
                            Text(" ${currentPost.commentCount}", color = Color.Black, modifier = Modifier.padding(end = 16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.toggleLike() }
                            ) {
                                Icon(
                                    imageVector = if (isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    tint = if (isLikedByMe) Color.Red else primary,
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = null
                                )
                                Text(" ${currentPost.likeCount}", color = Color.Black)
                            }

                            Spacer(modifier = Modifier.weight(1f))


                            if (currentPost.author.id == CurrentUser.uid) {
                                IconButton(onClick = {
                                    navController.navigate(Routes.CommunityPost(postId = currentPost.postId))
                                }) {
                                    Icon(painterResource(id = R.drawable.ic_edit), null, tint = Color.DarkGray, modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { viewModel.openDeleteDialog() }) {
                                    Icon(painterResource(id = R.drawable.ic_trash), null, tint = Color.DarkGray, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }


                    item {
                        CommentInputSection(
                            nickname = CurrentUser.nickname,
                            text = uiState.comment,
                            onTextChange = { viewModel.onCommentChange(it) },
                            onSend = { viewModel.addComment() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }


                    items(uiState.commentList) { commentData ->
                        CommentRow(name = commentData.user.nickname, profileImg = commentData.user.profileImg,content = commentData.content)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(name: String, profileImg:String, content: String) {
    Row(verticalAlignment = Alignment.Top) {
        ProfileImage(profileImg, 24)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
            Text(content, fontSize = 14.sp, color = Color.Black)
        }
    }
}

@Composable
fun CommentInputSection(nickname: String, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = textFieldBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(nickname, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth().height(70.dp).padding(top = 8.dp),
                placeholder = { Text("댓글을 남겨보세요...", fontSize = 13.sp, color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Button(
                    onClick = onSend,
                    modifier = Modifier.height(32.dp).padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("등록", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}