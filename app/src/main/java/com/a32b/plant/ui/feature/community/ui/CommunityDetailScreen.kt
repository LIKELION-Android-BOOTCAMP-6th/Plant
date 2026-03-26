package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.Post
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(navController: NavController) {
    val args = navController.currentBackStackEntry?.toRoute<Routes.CommunityList>()

//    val viewModel: CommunityDetailViewModel = viewModel(
//        factory = ViewModelFactory.communityDetailViewModelFactory(args.postId)
//    )



//    val postList by viewModel.uiState.collectAsStateWithLifecycle()
//    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }

//    Scaffold(
//        containerColor = Color(0xFFFDFDF0),
//        topBar = {
//            CommunitySearchBar(
//                query = searchQuery,
//                onQueryChange = { viewModel.onSearchQueryChanged(it) },
//                onFilterClick = { showDialog = true },
//                isFilterActive = selectedTags.isNotEmpty()
//            )
//        }
//,
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { navController.navigate(Routes.CommunityPost()) },
//                containerColor = Color(0xFFE6D5B8),
//                shape = CircleShape
//            ) {
//                Icon(painterResource(R.drawable.ic_edit), null, Modifier.size(24.dp))
//            }
//        }
//    )
//    {
//        innerPadding ->
//        if (postList.isEmpty()) {
//            EmptyView()
//        } else {
//            LazyColumn(
//                modifier = Modifier.padding(innerPadding).fillMaxSize(),
//                contentPadding = PaddingValues(16.dp),
//                verticalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                items(postList) { post ->
//                    PostItemCard(
//                        post = post,
//                        onClick = { navController.navigate(Routes.CommunityDetail(postId = post.id)) }
//                    )
//                }
//            }
//        }
//    }

//    if (showDialog) {
//        CategorySelectDialog(
//            selectedTags = selectedTags,
//            onTagClick = { tag ->
//                selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
//            },
//            onDismiss = { showDialog = false }
//        )
//    }
}

// --- 부품 1: 검색바 ---
@Composable
fun CommunitySearchBar(query: String, onQueryChange: (String) -> Unit, onFilterClick: () -> Unit, isFilterActive: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("검색어를 입력하세요", fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onFilterClick) {
                    Icon(painterResource(R.drawable.ic_community_filters), null, Modifier.size(22.dp),
                        tint = if (isFilterActive) Color(0xFF4CAF50) else Color.Gray)
                }
                Icon(Icons.Default.Search, null, tint = Color.Gray)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White),
        singleLine = true
    )
}

// --- 부품 2: 게시글 카드 ---
@Composable
fun PostItemCard(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = post.content, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFC5E1A5)))
                Text("  ${post.nickName}", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(12.dp))
//            Row {
//                StatInfo(R.drawable.ic_community_comment, post.commentCount.toString())
//                Spacer(Modifier.width(16.dp))
//                StatInfo(
//                    if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
//                    post.likeCount.toString(),
//                    if (post.isLiked) Color.Red else Color.Gray
//                )
//            }
        }
    }
}

@Composable
fun StatInfo(icon: Any, count: String, tint: Color = Color.Gray) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon is Int) Icon(painterResource(icon), null, Modifier.size(16.dp), tint)
        else if (icon is ImageVector) Icon(icon, null, Modifier.size(16.dp), tint)
        Text(" $count", fontSize = 12.sp, color = tint)
    }
}

// --- 부품 3: 카테고리 다이얼로그 ---
@Composable
fun CategorySelectDialog(selectedTags: Set<String>, onTagClick: (String) -> Unit, onDismiss: () -> Unit) {
    val tags = listOf("중학생", "고등학생", "취준", "자격증", "자랑", "취미", "공유")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리 선택", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            Surface(
                                modifier = Modifier.weight(1f).clickable { onTagClick(tag) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFC5E1A5) else Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF9CCC65) else Color.LightGray)
                            ) {
                                Box(Modifier.padding(vertical = 12.dp), Alignment.Center) {
                                    Text(tag, fontSize = 13.sp, color = if (isSelected) Color.Black else Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("적용하기") } }
    )
}

@Composable
fun EmptyView() {
    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("검색 결과가 없습니다 😭", color = Color.Gray) }
}
//}
