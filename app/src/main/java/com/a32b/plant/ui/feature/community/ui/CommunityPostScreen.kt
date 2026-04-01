package com.a32b.plant.ui.feature.community.ui

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.core.component.TagGroup
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostEvent
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
    val tag = args?.tag
    val title = args?.title
    val studyLogIds = args?.studyLogIds
    val viewModel: CommunityPostViewModel = viewModel(
        factory = ViewModelFactory.communityPostViewModelFactory(postId, potId, title,studyLogIds)
    )


    val uiState by viewModel.uiState.collectAsState()

    tag?.let {
        val tags = listOf(tag.dropLast(2), tag.takeLast(2))
        viewModel.onSelectedTagChange(tags)
    }

    LaunchedEffect(postId,  Unit) {
        postId?.let { viewModel.getPost(postId) }
        if(uiState.selected.contains("공유")){
            viewModel.onIsSharedChange()
        }

        viewModel.event.collect { event ->
            when(event){
                is CommunityPostEvent.NavigateToDetail -> {
                    navController.navigate(Routes.CommunityList)
                    navController.navigate(Routes.CommunityDetail(event.postId)){
                        popUpTo<Routes.CommunityPost> { inclusive = true }
                    }
                }
            }
        }
    }
    BackHandler { viewModel.onIsDismissDialogShowChange() }

    Scaffold(
        containerColor = background,
        topBar = {
            PostTopBar(
                isEditMode = postId != null,
                onBackClick = {
                    viewModel.onIsDismissDialogShowChange()
                },
                onRegisterClick = {
                    val isInvalid = uiState.title.isBlank() ||
                            (uiState.studyLogs == null && uiState.content.isNullOrBlank())
                    if (isInvalid) {
                        Toast.makeText(context, "제목과 내용을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                    } else if(uiState.selected.isEmpty()){
                        Toast.makeText(context, "태그를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    } else{
                        viewModel.savePost() { isSuccess ->
                            if (isSuccess) {
                                val msg = if (postId != null) "수정되었습니다!" else "성공적으로 등록되었습니다!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                Text("제목", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                PostInputField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    placeholder = "제목을 입력하세요",
                    singleLine = true,

                )
            }

            item {
                Text("태그", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TagGroup(uiState.tags + if(uiState.isShared) listOf("공유") else emptyList(), enable = !uiState.isShared, init = if(uiState.isShared)uiState.selected else emptyList()){ selected ->
                    viewModel.onSelectedTagChange(selected)

                }
            }

            if (uiState.isShared){
                val studyLogs = uiState.studyLogs ?: emptyList()
                item {
                    StudyLogCard(studyLogs)
                }
            }else{
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
            }


            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    if (uiState.isDismissDialogShow)
        ConfirmDialog("게시글 작성을 종료하시겠습니까?",
            semiText = "작성된 게시글은 저장되지 않습니다.",
            onDismiss = {viewModel.onIsDismissDialogShowChange()},
            onConfirm = {navController.popBackStack()
            }
        )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostTopBar(isEditMode: Boolean, onBackClick: () -> Unit, onRegisterClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(if (isEditMode) "글 수정" else "글쓰기", style = Typography.titleLarge) },
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
fun PostInputField(
    value: String?,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false
) {
    TextField(
        value = value ?: "",
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color.LightGray, style = Typography.bodyMedium) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        textStyle = Typography.bodyMedium
    )
}