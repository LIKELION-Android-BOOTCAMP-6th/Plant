package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel

// --- 데이터 모델 ---
data class Author(val nickname: String = "작성자")
data class Post(
    val id: String = "",
    val author: Author = Author(),
    val content: String = "",
    val commentCount: Int = 15,
    var likeCount: Int = 20,
    var isLiked: Boolean = false,
    val createdAt: String = "2026-03-25"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(
    navController: NavController,
    viewModel: CommunityListViewModel // 👈 ViewModel을 외부에서 받아옵니다.
) {
    // 1. ViewModel의 상태 관찰 (실시간 검색 리스트 및 검색어)
    val postList by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미","자랑", "공유")

    // 2. 카테고리 선택 다이얼로그
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "카테고리 선택", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filterTags.chunked(2).forEach { rowTags ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowTags.forEach { tag ->
                                val isSelected = selectedTags.contains(tag)
                                Surface(
                                    modifier = Modifier.weight(1f).clickable {
                                        selectedTags = if (isSelected) selectedTags - tag else selectedTags + tag
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFC5E1A5) else Color(0xFFF5F5F5),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF9CCC65) else Color.LightGray)
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                        Text(text = tag, color = if (isSelected) Color.Black else Color.Gray, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("적용하기", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Surface(modifier = Modifier.statusBarsPadding(), color = Color.Transparent) {
                OutlinedTextField(
                    value = searchQuery, // ✅ ViewModel 검색어 연결
                    onValueChange = { viewModel.onSearchQueryChanged(it) }, // ✅ 글자 변경 시 ViewModel 호출
                    placeholder = { Text("검색어를 입력하세요", fontSize = 15.sp) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(64.dp), // ✅ 한글 씹힘 방지 높이
                    textStyle = TextStyle(fontSize = 16.sp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusRequester.freeFocus() }),
                    shape = RoundedCornerShape(12.dp),
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
                                modifier = Modifier.size(24.dp).clickable { showDialog = true }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        }
                    },
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CommunityPost()) },
                containerColor = Color(0xFFE6D5B8),
                shape = CircleShape
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        // 3. 필터링된 리스트 결과 출력
        if (postList.isEmpty()) {
            // 💡 검색 결과가 없을 때 보여줄 화면
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "검색 결과가 없습니다 😭", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(postList) { index, post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.CommunityDetail(postId = post.id)) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(text = post.content, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = post.createdAt, fontSize = 11.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFC5E1A5)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = post.author.nickname, fontSize = 12.sp, color = Color.DarkGray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // 댓글 아이콘
                                Icon(painter = painterResource(id = R.drawable.ic_community_comment), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Text(text = " ${post.commentCount}", fontSize = 11.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.width(16.dp))

                                // ✅ 좋아요 클릭 및 색상 변경 로직
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        // 좋아요 클릭 시 ViewModel이나 Repository를 통해 실제 데이터를 바꿔야 함
                                        // 우선 UI 동작을 위해 클릭 이벤트만 전파 가능
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (post.isLiked) Color.Red else Color.Gray
                                    )
                                    Text(
                                        text = " ${post.likeCount}",
                                        fontSize = 11.sp,
                                        color = if (post.isLiked) Color.Red else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}