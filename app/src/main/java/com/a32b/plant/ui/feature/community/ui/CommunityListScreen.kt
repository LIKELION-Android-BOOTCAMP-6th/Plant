package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// ✅ LayoutId::class를 삭제하고 ExperimentalLayoutApi::class를 추가했습니다.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityListScreen(navController: NavController) {

    // ✅ 1. 데이터 클래스 및 보조 함수를 내부로 이동 (캡슐화)
    fun getCurrentDate(): String {
        val date = Calendar.getInstance().time
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }

    data class Author(
        val id: String = "",
        val nickname: String = "",
        val profileImg: String = ""
    )

    data class Post(
        val id: String = "",
        val author: Author = Author(),
        val content: String = "",
        val tag: List<String> = emptyList(),
        val commentCount: Int = 0,
        val likeCount: Int = 0,
        val createdAt: String = getCurrentDate()
    )

    @Composable
    fun FeedListItem(post: Post) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // ✅ 게시글 클릭 시 상세 페이지로 이동
                    navController.navigate(Routes.CommunityPost())
                },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F9)),
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
                    Icon(painter = painterResource(id = R.drawable.ic_community_comment), contentDescription = "댓글", modifier = Modifier.size(18.dp))
                    Text(text = " ${post.commentCount}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(16.dp))
                    Icon(painter = painterResource(id = R.drawable.ic_community_like_normal), contentDescription = "좋아요", modifier = Modifier.size(18.dp))
                    Text(text = " ${post.likeCount}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    // --- 상태 관리 ---
    val focusRequester = remember { FocusRequester() }
    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // ✅ 중복 선택을 위한 Set 상태
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미", "공유")

    val postList = listOf(
        Post(id = "1", author = Author(nickname = "식물집사1"), content = "요즘 국어 왤케 어렵냐", likeCount = 20, commentCount = 15),
        Post(id = "2", author = Author(nickname = "초보가드너"), content = "몬스테라 잎이 노랗게 변해요 ㅠㅠ", likeCount = 12, commentCount = 8)
    )

    // ✅ 필터 다이얼로그 (2열 배치 반영)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "카테고리 선택 (중복 가능)", fontWeight = FontWeight.Bold) },
            text = {
                // ✅ FlowRow를 사용하여 2열로 자동 배치
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2 // 한 행에 2개씩
                ) {
                    filterTags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.47f) // 약 절반 너비로 설정하여 2열 구성
                                .clickable {
                                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFC5E1A5) else Color(0xFFF5F5F5),
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.Black else Color.Gray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF4E342E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("적용하기", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTags = emptySet() }) {
                    Text("초기화", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.statusBarsPadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("검색", style = MaterialTheme.typography.bodyMedium) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        .height(50.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusRequester.freeFocus() }),
                    trailingIcon = {
                        Row(modifier = Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_community_filters),
                                    contentDescription = "필터",
                                    modifier = Modifier.size(24.dp).clickable { showDialog = true },
                                    tint = if (selectedTags.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray
                                )
                                // ✅ 선택된 필터 개수 배지
                                if (selectedTags.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                                        color = Color.Red,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = selectedTags.size.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "검색",
                                modifier = Modifier.clickable { focusRequester.requestFocus() }
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
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
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = "글쓰기", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(postList.size) { index ->
                FeedListItem(postList[index])
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CommunityListPreview() {
    CommunityListScreen(navController = rememberNavController())
}