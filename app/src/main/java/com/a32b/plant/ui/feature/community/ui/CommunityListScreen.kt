package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ⏰ 날짜 함수
fun getCurrentDate(): String {
    val date = Calendar.getInstance().time
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(date)
}

// 📊 데이터 모델 (에러 방지를 위해 이 파일 전용으로 이름 변경)
data class ListAuthor(val id: String = "", val nickname: String = "작성자", val profileImg: String = "")
data class ListPost(val id: String = "", val author: ListAuthor = ListAuthor(), val content: String = "", val tag: List<String> = emptyList(), val commentCount: Int = 15, val likeCount: Int = 20, val createdAt: String = "2026-03-16")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(navController: NavController) {

    // --- 상태 관리 ---
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미", "공유")

    // 임시 데이터 (ListPost 사용)
    val postList = listOf(
        ListPost(id = "1", content = "요즘 국어 왤케 어렵냐"),
        ListPost(id = "2", content = "요즘 국어 왤케 어렵냐")
    )

    // 💡 다이얼로그 (필터)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "카테고리 선택", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    filterTags.forEach { tag ->
                        TextButton(onClick = { showDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(text = tag, color = Color(0xFF4E342E))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text("닫기") } }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                color = Color.Transparent
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("검색", style = MaterialTheme.typography.bodyMedium, color = Color.LightGray) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(50.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusRequester.freeFocus() }),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFF9575CD)
                    ),
                    trailingIcon = {
                        Row(modifier = Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_community_filters),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).clickable { showDialog = true },
                                tint = Color(0xFF9575CD)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.clickable { focusRequester.requestFocus() },
                                tint = Color.Gray
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CommunityPost()) },
                modifier = Modifier.padding(bottom = 16.dp),
                containerColor = Color(0xFFE6D5B8),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(postList.size) { index ->
                FeedListItem(postList[index], onItemClick = {
                    navController.navigate(Routes.CommunityDetail(postId = postList[index].id))
                })
            }
        }
    }
}

@Composable
fun FeedListItem(post: ListPost, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = if (post.id == "1") Color(0xFFE8F5E9) else Color(0xFFF5F6F9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = post.content, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = post.createdAt, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = Color(0xFFC5E1A5)) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = post.author.nickname, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.ic_community_comment), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Text(text = " ${post.commentCount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(painter = painterResource(id = R.drawable.ic_community_like_normal), contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                Text(text = " ${post.likeCount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CommunityListPreview() {
    CommunityListScreen(navController = rememberNavController())
}
