package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes

// --- 📊 1. 데이터 모델 (태그 필드 추가) ---
data class Author(val nickname: String = "작성자")
data class Post(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(), // ✅ 게시글이 가진 태그들
    val author: Author = Author(),
    val date: String = "yy-mm-dd",
    val commentCount: Int = 15,
    var likeCount: Int = 20,
    var isLiked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(navController: NavController) {
    // 2. 임시 데이터 (필터링 테스트를 위해 태그를 넣었습니다)
    val allPosts = remember {
        mutableStateListOf(
            Post("1", "요즘 국어 왤케 어렵냐", listOf("고등학생")),
            Post("2", "식물 키우는 법 공유해요", listOf("공유", "취미")),
            Post("3", "자격증 한 달 만에 따기", listOf("자격증")),
            Post("4", "취준생들 힘내세요!", listOf("취준"))
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미", "공유")

    // ✅ 💡 중복 선택을 위한 장바구니 상태 (Set)
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

    // ✅ 💡 필터링 로직: 검색어와 선택된 태그를 모두 반영합니다!
    val filteredPosts = remember(searchQuery, selectedTags, allPosts.toList()) {
        allPosts.filter { post ->
            // 검색어 포함 여부
            val matchesSearch = post.title.contains(searchQuery, ignoreCase = true)
            // 선택된 태그 중 하나라도 글의 태그에 포함되어 있는지 (비어있으면 전체 노출)
            val matchesTags = selectedTags.isEmpty() || post.tags.any { it in selectedTags }

            matchesSearch && matchesTags
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    // 다이얼로그 (중복 체크 가능 + 2열 배치)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "카테고리 선택", fontWeight = FontWeight.Bold) },
            text = {
                // ✅ FlowRow를 사용하여 2열(또는 그 이상)로 배치
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2 // 한 줄에 최대 2개씩 배치
                ) {
                    filterTags.forEach { tag ->
                        val isSelected = selectedTags.contains(tag)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.47f) // 약 절반 너비로 설정하여 2열 유도
                                .clickable {
                                    // 💡 토글 로직: 있으면 빼고, 없으면 넣기
                                    selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFC5E1A5) else Color(0xFFF5F5F5),
                            border = if (isSelected) null else BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tag,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.DarkGray else Color.Gray
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("적용하기", fontWeight = FontWeight.Bold, color = Color(0xFF9575CD))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTags = emptySet() }) {
                    Text("초기화", color = Color.Gray)
                }
            },
            containerColor = Color(0xFFFDFCFB),
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Column(modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    placeholder = { Text("검색", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedBorderColor = Color(0xFF9575CD)
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Box(
                                modifier = Modifier.size(32.dp)
                                    .border(1.dp, Color(0xFF9575CD), RoundedCornerShape(4.dp))
                                    .clickable { showDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_community_filters),
                                    contentDescription = "필터",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF9575CD)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "검색",
                                modifier = Modifier.size(24.dp).clickable { keyboardController?.hide() },
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
                containerColor = Color(0xFFD7CCC8),
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = "글쓰기", modifier = Modifier.size(28.dp), tint = Color.Black)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredPosts, key = { it.id }) { post ->
                FeedListItem(post, onLikeClick = {
                    val index = allPosts.indexOfFirst { it.id == post.id }
                    if (index != -1) {
                        val target = allPosts[index]
                        val newLikedStatus = !target.isLiked
                        allPosts[index] = target.copy(isLiked = newLikedStatus, likeCount = if (newLikedStatus) target.likeCount + 1 else target.likeCount - 1)
                    }
                }, onItemClick = {
                    navController.navigate(Routes.CommunityDetail(postId = post.id))
                })
            }
        }
    }
}

@Composable
fun FeedListItem(post: Post, onLikeClick: () -> Unit, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onItemClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = post.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(text = post.date, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFFC5E1A5)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = post.author.nickname, fontSize = 13.sp, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.ic_community_comment), null, Modifier.size(16.dp), tint = Color.Gray)
                Text(" ${post.commentCount}", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Row(modifier = Modifier.clickable { onLikeClick() }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (post.isLiked) Color.Red else Color.Gray
                    )
                    Text(" ${post.likeCount}", fontSize = 12.sp, color = if (post.isLiked) Color.Red else Color.Gray)
                }
            }
        }
    }
}
