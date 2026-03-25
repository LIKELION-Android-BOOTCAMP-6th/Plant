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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel // ✅ ViewModel 위치에 맞게 수정하세요

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavController,
    viewModel: CommunityListViewModel // ✅ 이제 ViewModel을 받아서 사용합니다.
) {
    // 1️⃣ DB에서 가져온 데이터 및 검색어 상태 관찰 (실시간)
    val postList by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "자랑", "취미", "공유")

    // 2️⃣ 카테고리 선택 다이얼로그 (기존 로직 유지)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = "카테고리 선택", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filterTags.chunked(2).forEach { rowTags ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        Text(text = tag, fontSize = 13.sp, color = if (isSelected) Color.Black else Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("적용하기", color = Color(0xFF4E342E), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Surface(modifier = Modifier.statusBarsPadding(), color = Color.Transparent) {
                OutlinedTextField(
                    value = searchQuery, // ✅ ViewModel의 검색어와 연결
                    onValueChange = { viewModel.onSearchQueryChanged(it) }, // ✅ 입력 시 ViewModel 함수 호출
                    placeholder = { Text("검색어를 입력하세요", fontSize = 15.sp) },
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .height(64.dp),
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
                                modifier = Modifier.size(24.dp).clickable { showDialog = true },
                                tint = if (selectedTags.isNotEmpty()) Color(0xFF4CAF50) else Color.Gray
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
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_edit), contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    ) { innerPadding ->
        // 3️⃣ DB 리스트 영역 (검색 결과가 없을 때 처리 추가)
        if (postList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "게시글이 없거나 검색 결과가 없습니다.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(postList) { _, post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Routes.CommunityDetail(postId = post.id)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = post.content, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            // 작성자 정보
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFC5E1A5)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = post.author.nickname, fontSize = 12.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4️⃣ 댓글 및 좋아요 정보 (아이콘 추가)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(R.drawable.ic_community_comment), contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Text(text = " ${post.commentCount}", fontSize = 12.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.width(16.dp))

                                // 좋아요 영역 (눌렀을 때 동작은 ViewModel에 구현 필요)
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (post.isLiked) Color.Red else Color.Gray
                                )
                                Text(text = " ${post.likeCount}", fontSize = 12.sp, color = if (post.isLiked) Color.Red else Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}