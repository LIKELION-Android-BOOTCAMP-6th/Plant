package com.a32b.plant.ui.feature.community.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.core.component.Tag
import com.a32b.plant.data.model.Comment

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


    // 게시글 삭제 다이얼로그 (core/component/ConfirmDialog.kt - 다이얼로그 공통소스 ConfirmDialog로 변경)
    if (showDeleteDialog) {
        ConfirmDialog(
            text = "게시글을 삭제하시겠습니까?",
            onDismiss = { viewModel.closeDeleteDialog() },
            onConfirm = {
                viewModel.deletePost { navController.popBackStack() }
                viewModel.closeDeleteDialog()
            }
        )
    }

    // 댓글 삭제 다이얼로그
    if (uiState.deletingCommentId != null) {
        ConfirmDialog(
            text = "댓글을 삭제하시겠습니까?",
            onDismiss = { viewModel.closeCommentDeleteDialog() },
            onConfirm = { viewModel.deleteComment() }
        )
    }

    Scaffold(
        containerColor = background,
    ) { innerPadding ->
        val currentPost = postState ?: return@Scaffold

        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(horizontal = 20.dp)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray),
                elevation = CardDefaults.elevatedCardElevation(1.dp)
            ) {
                LazyColumn(modifier = Modifier.padding(20.dp)) {

                    item {
                        Box(modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center){
                            IconButton(onClick = {navController.popBackStack()},
                                modifier = Modifier.size(30.dp).align(Alignment.CenterStart)
                            ) {
                                Image(painter = painterResource(R.drawable.ic_backbtn),
                                    contentDescription = "뒤로가기")
                            }
                            Text(currentPost.title, fontSize = 24.sp,style = Typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 46.dp),
                                textAlign = TextAlign.Center)

                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(currentPost.author.profileImg, 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(currentPost.author.nickname,  style = Typography.bodyMedium)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(TimeFormatter.formatTimestamp(currentPost.createdAt), style = Typography.bodyMedium, fontSize = 12.sp)
                        }
                    }
                    item{
                        Row(modifier = Modifier.padding(start = 7.dp)) {
                            for (tag in currentPost.tag) {
                                Tag(tag)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                    }

                    if(!currentPost.studyLogs.isNullOrEmpty()){
                        item {
                            StudyLogCard(currentPost.studyLogs)
                        }
                    }else{
                        item {
                        Text(currentPost.content?:"", style = Typography.bodyMedium)

                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(30.dp))

                        HorizontalDivider(color = sub3.copy(alpha = 0.5f))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Icon(painterResource(id = R.drawable.ic_community_comment), null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Text(" ${currentPost.commentCount}", style = Typography.bodyMedium, modifier = Modifier.padding(end = 16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { viewModel.toggleLike() }
                            ) {
                                Icon(
                                    painterResource(id = if (isLikedByMe) R.drawable.ic_community_like_selected else R.drawable.ic_community_like_normal),
                                    tint = if (isLikedByMe) primary else Color.Gray,
                                    modifier = Modifier.size(18.dp),
                                    contentDescription = null
                                )
                                Text(" ${currentPost.likeCount}", style = Typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.weight(1f))


                            if (currentPost.author.id == CurrentUser.uid) {
                                IconButton(onClick = {
                                    navController.navigate(Routes.CommunityPost(postId = currentPost.postId))
                                }) {
                                    Icon(painterResource(id = R.drawable.ic_edit), null, tint = Color.DarkGray, modifier = Modifier.size(18.dp))
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

                    // CommentRow에 수정/삭제 기능 파라미터 전달
                    items(uiState.commentList) { commentData ->
                        CommentRow(
                            comment = commentData,
                            isOwner = commentData.user.uid == CurrentUser.uid,
                            isEditing = uiState.editingCommentId == commentData.commentId,
                            editingText = uiState.editingCommentText,
                            onEditTextChange = { viewModel.onEditCommentTextChange(it) },
                            onEditStart = { viewModel.startEditComment(commentData) },
                            onEditSubmit = { viewModel.submitEditComment() },
                            onEditCancel = { viewModel.cancelEditComment() },
                            onDeleteClick = { viewModel.openCommentDeleteDialog(commentData.commentId) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// CommentRow에 인라인 편집 모드 + 수정/삭제 아이콘 추가
@Composable
fun CommentRow(
    comment: Comment,
    isOwner: Boolean,
    isEditing: Boolean,
    editingText: String,
    onEditTextChange: (String) -> Unit,
    onEditStart: () -> Unit,
    onEditSubmit: () -> Unit,
    onEditCancel: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val maxLength = 100

    Row(verticalAlignment = Alignment.Top) {
        ProfileImage(comment.user.profileImg, 24)
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            // 닉네임 + 작성 시간을 한 줄에 표시
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.user.nickname, fontWeight = FontWeight.Bold, fontSize = 13.sp, style = Typography.bodyMedium)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = comment.createdAt?.let { TimeFormatter.formatTimestampTime(it) } ?: "",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    style = Typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(7.dp))


            if (isEditing) {
                // 인라인 편집 모드
                TextField(
                    value = editingText,
                    onValueChange = { input ->
                        limitLength(input, maxLength, context, onEditTextChange)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = Typography.bodyMedium
                )
                Row {
                    TextButton(onClick = onEditSubmit) {
                        Text("저장", color = primary, fontSize = 12.sp,style = Typography.bodyMedium)
                    }
                    TextButton(onClick = onEditCancel) {
                        Text("취소", color = Color.Gray, fontSize = 12.sp,style = Typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "${editingText.length} / $maxLength",
                        fontSize = 12.sp,
                        color = if (editingText.length >= maxLength) Color.Red else Color.Gray,
                        style = Typography.bodyMedium,
                        modifier = Modifier.padding(end = 40.dp)
                    )
                }
            } else {
                Text(comment.content, fontSize = 14.sp, style = Typography.bodyMedium)
            }
        }

        // 본인 댓글이고 편집 중이 아닐 때만 수정/삭제 아이콘 표시
        if (isOwner && !isEditing) {
            IconButton(onClick = onEditStart, modifier = Modifier.size(28.dp)) {
                Icon(
                    painterResource(id = R.drawable.ic_edit), null,
                    tint = Color.Gray, modifier = Modifier.size(14.dp)
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                Icon(
                    painterResource(id = R.drawable.ic_trash), null,
                    tint = Color.Gray, modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CommentInputSection(nickname: String, text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    val context = LocalContext.current
    val maxLength = 100
    val focus = LocalFocusManager.current
    Card(
        colors = CardDefaults.cardColors(containerColor = textFieldBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(nickname, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = Typography.bodyMedium)
            TextField(
                value = text,
                onValueChange = { input ->
                    limitLength(input, maxLength, context, onTextChange)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(top = 8.dp),
                placeholder = { Text("댓글을 남겨보세요...", fontSize = 13.sp, color = Color.Gray, style = Typography.bodyMedium) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${text.length} / $maxLength",
                    fontSize = 12.sp,
                    color = if (text.length >= maxLength) Color.Red else Color.Gray,
                    style = Typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {onSend()
                              focus.clearFocus()},
                    modifier = Modifier
                        .height(32.dp)
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("등록", fontSize = 12.sp, color = Color.White,style = Typography.bodyMedium)
                }
            }
        }
    }
}

fun limitLength(input: String, maxLength: Int, context: Context, onValueChange: (String) -> Unit){
    if (input.length <= maxLength) {
        onValueChange(input)
    } else {
        Toast.makeText(context, "${maxLength}자 이하로 입력해주세요.", Toast.LENGTH_SHORT).show()
    }
}