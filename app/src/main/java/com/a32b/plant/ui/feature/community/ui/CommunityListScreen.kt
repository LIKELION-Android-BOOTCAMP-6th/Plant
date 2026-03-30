package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.a32b.plant.R
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.Post
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel
import java.text.SimpleDateFormat
import java.util.*

fun formatTimeAgo(dateString: String): String {
    return try {
        // 데이터베이스의 날짜 형식에 맞춰 포맷 지정 (yyyy-MM-dd HH:mm 기준)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        val date = sdf.parse(dateString) ?: return dateString
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            seconds < 60 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityListScreen(navController: NavController) {
    val viewModel: CommunityListViewModel = viewModel(factory = ViewModelFactory.communityListViewModelFactory)
    val postList by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTags by viewModel.selectedTags.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미", "자랑", "공유")

    Scaffold(
        containerColor = Color(0xFFFDFDF0),
        topBar = {
            Column(modifier = Modifier.background(Color(0xFFFDFDF0))) {
                SearchBarSection(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onFilterClick = { showDialog = true },
                    isFilterActive = selectedTags.isNotEmpty()
                )

                TagRowSection(
                    tags = filterTags,
                    selectedTags = selectedTags,
                    onTagClick = { tag ->
                        val newSelection = if (selectedTags.contains(tag)) {
                            selectedTags - tag
                        } else {
                            selectedTags + tag
                        }
                        viewModel.onTagsChanged(newSelection)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CommunityPost()) },
                containerColor = Color(0xFFE6D5B8),
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Color.Black
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (postList.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(postList) { post ->
                        PostCard(
                            post = post,
                            onClick = { navController.navigate(Routes.CommunityDetail(postId = post.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            currentSelected = selectedTags,
            onDismiss = { showDialog = false },
            onApply = { newSelection ->
                viewModel.onTagsChanged(newSelection)
                showDialog = false
            }
        )
    }
}

@Composable
fun TagRowSection(
    tags: List<String>,
    selectedTags: Set<String>,
    onTagClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(tags) { tag ->
            val isSelected = selectedTags.contains(tag)
            Surface(
                modifier = Modifier.clickable { onTagClick(tag) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color(0xFFC5E1A5) else Color.White,
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF9CCC65) else Color(0xFFE0E0E0))
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,

                    color = if (isSelected) Color(0xFF33691E) else Color.Black,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit, onFilterClick: () -> Unit, isFilterActive: Boolean) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("검색어를 입력하세요", fontSize = 14.sp, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_community_filters),
                        contentDescription = "필터",
                        modifier = Modifier.size(22.dp),
                        tint = if (isFilterActive) Color(0xFF4CAF50) else Color.Black // ✅ 필터 블랙
                    )
                }
                Icon(imageVector = Icons.Default.Search, contentDescription = "검색", modifier = Modifier.size(24.dp), tint = Color.Black) // ✅ 검색 블랙
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        ),
        singleLine = true
    )
}

@Composable
fun CategoryDialog(
    currentSelected: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var tempSelected by remember { mutableStateOf(currentSelected) }
    val filterTags = listOf("중학생", "고등학생", "취준", "자격증", "취미", "자랑", "공유")
    val itemsPerRow = (filterTags.size + 1) / 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("카테고리 중복 선택", fontWeight = FontWeight.Bold, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filterTags.chunked(itemsPerRow).forEach { rowTags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTags.forEach { tag ->
                            val isSelected = tempSelected.contains(tag)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        tempSelected = if (isSelected) tempSelected - tag else tempSelected + tag
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFC5E1A5) else Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF9CCC65) else Color.LightGray)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onApply(tempSelected) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9CCC65))) {
                Text("적용하기", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Black) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun PostCard(post: Post, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text(text = post.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                Text(text = formatTimeAgo(post.createdAt), fontSize = 11.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFC5E1A5)))
                Text(text = "  ${post.nickName}", fontSize = 12.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                IconStat(R.drawable.ic_community_comment, post.commentCount.toString())
                Spacer(modifier = Modifier.width(12.dp))

                val isLiked = post.isLiked
                IconStat(
                    iconRes = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    text = post.likeCount.toString(),
                    tint = if (isLiked) Color.Red else Color.Black
                )
            }
        }
    }
}

@Composable
fun IconStat(iconRes: Any, text: String, tint: Color = Color.Black) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (iconRes) {
            is Int -> Icon(painterResource(iconRes), null, Modifier.size(14.dp), tint)
            is ImageVector -> Icon(iconRes, null, Modifier.size(14.dp), tint)
        }
        Text(text = " $text", fontSize = 11.sp, color = tint)
    }
}

@Composable
fun EmptyStateView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("찾으시는 게시글이 없어요 😭", color = Color.Black)
    }
}