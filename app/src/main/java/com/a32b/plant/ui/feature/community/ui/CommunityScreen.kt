package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController) {

    // ✅ 1. 데이터 클래스 및 보조 함수 내부 선언
    data class Author(
        val nickname: String = "작성자",
        val profileImg: String = ""
    )

    data class Post(
        val id: String,
        val title: String,
        val author: Author = Author(),
        val date: String = "2026-03-16",
        val commentCount: Int = 15,
        var likeCount: Int = 20,
        var isLiked: Boolean = false
    )

    val allPosts = remember {
        mutableStateListOf(
            Post("1", "요즘 국어 왤케 어렵냐"),
            Post("2", "식물 키우는 법 공유해요"),
            Post("3", "오늘 점심 메뉴 추천"),
            Post("4", "안드로이드 개발 팁")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredPosts = remember(searchQuery, allPosts.toList()) {
        if (searchQuery.isBlank()) allPosts.toList()
        else allPosts.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    val performSearch = {
        keyboardController?.hide()
    }

    @Composable
    fun FeedListItem(post: Post) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(Routes.CommunityPost)
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F8E9)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = post.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = post.date,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFC5E1A5), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = post.author.nickname,
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_community_comment),
                            contentDescription = "댓글",
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = " ${post.commentCount}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val index = allPosts.indexOfFirst { it.id == post.id }
                            if (index != -1) {
                                val target = allPosts[index]
                                val newLikedStatus = !target.isLiked
                                allPosts[index] = target.copy(
                                    isLiked = newLikedStatus,
                                    likeCount = if (newLikedStatus) target.likeCount + 1 else target.likeCount - 1
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "좋아요",
                            modifier = Modifier.size(16.dp),
                            tint = if (post.isLiked) Color.Red else Color.Gray
                        )
                        Text(
                            text = " ${post.likeCount}",
                            fontSize = 12.sp,
                            color = if (post.isLiked) Color.Red else Color.Gray
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Transparent
            ) {
                // ✅ 아주 큰 검색창 디자인 (BasicTextField)
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp)),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        textAlign = TextAlign.Center // 🚀 입력 텍스트 가로 중앙 정렬 추가
                    ),
                    cursorBrush = SolidColor(Color.Black),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    decorationBox = { innerTextField ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically // 수직 중앙 배치
                        ) {
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center // 🚀 내부 요소 가로/세로 중앙 정렬
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "검색어를 입력하세요...",
                                        color = Color.LightGray,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center // 🚀 힌트 문구 중앙 정렬
                                    )
                                }
                                innerTextField()
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 필터 아이콘
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_community_filters),
                                    contentDescription = "필터",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { /* 필터 로직 */ },
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(12.dp))

                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "검색",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { performSearch() },
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CommunityPost) },
                containerColor = Color(0xFFE6D5B8),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "글쓰기",
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF5D4037)
                )
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
            items(filteredPosts, key = { it.id }) { post ->
                FeedListItem(post)
            }
        }
    }
}