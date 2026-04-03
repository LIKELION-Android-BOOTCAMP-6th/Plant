package com.a32b.plant.ui.feature.community.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.component.TagChip
import com.a32b.plant.core.component.TagSheet
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.Post
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1


@Composable
fun CommunityListScreen(navController: NavController) {
    val viewModel: CommunityListViewModel = viewModel(factory = ViewModelFactory.communityListViewModelFactory)
    val postList by viewModel.searchUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsState()

//    val tagList : List<Tag> = listOf(Tag(name = "국어", parentId = "1"),Tag(name = "영어", parentId = "1"),Tag(name = "기타", parentId = "1"),
//                                Tag(name = "자소서/이력서", parentId = "2"),Tag(name = "면접", parentId = "2"),Tag(name = "포트폴리오", parentId = "2"),Tag(name = "기타", parentId = "2"),
//                                    Tag(name = "중등국어", parentId = "3"),Tag(name = "중등영어", parentId = "3"),Tag(name = "기타", parentId = "3"),)

    BackHandler {
        navController.navigate(Routes.HomeMain) {
            popUpTo(Routes.HomeMain) { inclusive = false }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(10.dp)) {
                SearchBarSection(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )
                Row() {
                    Text("태그", style = Typography.titleSmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp,),
                        color = MaterialTheme.colorScheme.onBackground)
                    Icon(painter = painterResource(id = if(uiState.isTagSheetShown) R.drawable.ic_up else R.drawable.ic_down),
                        contentDescription = "태그박스",
                        modifier = Modifier.clickable{
                            viewModel.onIsTagSheetShownChange()
                        })
                    uiState.selected.forEach { tag ->
                        Text(tag.name, style = Typography.bodyMedium, fontSize = 13.sp,
                            modifier = Modifier.padding(3.dp))
                    }
                }
                if(uiState.isTagSheetShown){
                    TagSheet(uiState.tags, isMultiSelected = true, init = uiState.selected) { selected ->
                        viewModel.onSelectedChanged(selected.toList())
                        Log.d("선택된 거 ", selected.toList().toString())
                    }
                }

//                TagGroup(tags = uiState.tags + listOf("공유")){ selected ->
//                    viewModel.onSelectedChanged(selected.toList())
//
//                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CommunityPost()) },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)) {
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
                            isLiked = post.isLiked,
                            onClick = { navController.navigate(Routes.CommunityDetail(postId = post.postId)) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SearchBarSection(query: String, onQueryChange: (String) -> Unit) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("검색어를 입력하세요", style = Typography.bodyMedium,
//            color = Color.Gray,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp).height(56.dp),
        shape = RoundedCornerShape(12.dp),
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_community_clear), contentDescription = "초기화",
                    modifier = Modifier.size(24.dp).clickable {
                        onQueryChange("")
                        focus.clearFocus()
                    }, tint = Color.Black)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedTextColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        singleLine = true
    )
}

@Composable
fun PostCard(post: Post, isLiked: Boolean,onClick: () -> Unit ) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
//            Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text(text = post.title, fontWeight = FontWeight.Bold, style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text(text = TimeFormatter.formatTimeAgo(post.createdAt), fontSize = 11.sp, style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                if (post.isShared?:false) TagChip("공유", 10)
                TagChip(post.tag.name, 10)
            }

            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileImage(level = post.author.profileImg, 16)
                Text(text = "  ${post.author.nickname}", fontSize = 12.sp, style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                IconStat(R.drawable.ic_community_comment, post.commentCount.toString())
                Spacer(modifier = Modifier.width(12.dp))


                IconStat(
                    iconRes = if (isLiked) R.drawable.ic_community_like_selected else R.drawable.ic_community_like_normal,
                    text = post.likeCount.toString(),
                    tint = if (isLiked) primary else Color.Black
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