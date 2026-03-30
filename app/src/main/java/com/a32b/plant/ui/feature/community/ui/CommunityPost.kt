package com.a32b.plant.ui.feature.community.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background

@Composable
fun CommunityPostScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val args = navController.currentBackStackEntry?.toRoute<Routes.CommunityPost>()

    val postId = args?.postId
    val potId = args?.potId
    val studyLog = args?.studyLogId
    val viewModel: CommunityPostViewModel = viewModel(
        factory = ViewModelFactory.communityPostViewModelFactory(postId, potId, studyLog)
    )

    val uiState by viewModel.uiState.collectAsState()

    val tags = listOf("중학생", "고등학생", "대학생","취준", "자격증")


    LaunchedEffect(postId, potId) {
        postId?.let { viewModel.getPost(postId) }
        potId?.let { viewModel.getStudyLog(potId) }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            PostTopBar(
                isEditMode = postId != null,
                onBackClick = {
                    //⭐백버튼 클릭 시 정말 종료하겠냐는 다이얼로그 띄우기
                },
                onRegisterClick = {
                    if (uiState.title.isBlank() || uiState.content.isBlank()) {
                        Toast.makeText(context, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.savePost() { isSuccess ->
                            if (isSuccess) {
                                val msg = if (postId != null) "수정되었습니다!" else "성공적으로 등록되었습니다!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                //⭐등록 완료 시 해당 게시물 디테일로 넘어가기
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            item {
                Text("제목", style = Typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PostInputField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    placeholder = "제목을 입력하세요",
                    singleLine = true
                )
            }

            item {
                Text("카테고리", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
//                TagSelector(tags, selectedTag) { selectedTag = it }
            }

            item {
                Text("본문", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PostInputField(
                    value = uiState.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    placeholder = "나누고 싶은 이야기를 적어주세요.\n\n※ 비방이나 욕설은 제재 대상이 될 수 있습니다.",
                    modifier = Modifier.heightIn(min = 400.dp)
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTopBar(isEditMode: Boolean, onBackClick: () -> Unit, onRegisterClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(if (isEditMode) "글 수정" else "글쓰기", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.DarkGray)
            }
        },
        actions = {
            Surface(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clickable { onRegisterClick() },
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFC5E1A5)
            ) {
                Text(
                    text = if (isEditMode) "수정" else "등록",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF33691E)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun TagSelector(tags: List<String>, selectedTag: String, onTagSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tags) { tag ->
            val isSelected = tag == selectedTag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFFC5E1A5) else Color.White)
                    .border(1.dp, if (isSelected) Color(0xFF9CCC65) else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                    .clickable { onTagSelected(tag) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 12.sp,
                    color = if (isSelected) Color(0xFF33691E) else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun PostInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray, fontSize = 14.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}