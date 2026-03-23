package com.a32b.plant.ui.feature.community.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.a32b.plant.R

// 📊 댓글 데이터 설계도 (이름을 깔끔하게 Comment로 변경)
data class Comment(
    val authorNickname: String = "닉네임",
    val content: String = "댓글내용",
    val authorProfileImg: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostScreen(navController: NavController) {
    
    // 💡 이제 우리가 만든 Comment 설계도를 정상적으로 사용합니다.
    val commentList = listOf(
        Comment(authorNickname = "닉네임", content = "댓글내용")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 1. 게시글 본문 영역
            item {
                PostDetailContent()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 댓글 작성 창
            item {
                CommentInputSection()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. 댓글 리스트 영역
            items(commentList) { comment ->
                CommentItem(comment)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// 🧩 게시글 본문 컴포넌트
@Composable
fun PostDetailContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "작성글 제목",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = Color.LightGray) {}
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = "닉네임", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(text = "2026년03월16일", color = Color.Gray, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row {
            repeat(2) {
                Surface(
                    modifier = Modifier.size(width = 40.dp, height = 20.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.LightGray.copy(alpha = 0.5f)
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "작성글\n어쩌고 저쩌고", minLines = 5)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.ic_community_comment), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = " 15", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Icon(painter = painterResource(id = R.drawable.ic_community_like_normal), contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = " 20", fontSize = 14.sp)
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { }, modifier = Modifier.size(24.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
    }
}

// 🧩 댓글 입력창 컴포넌트
@Composable
fun CommentInputSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F1F1))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(16.dp), shape = CircleShape, color = Color.LightGray) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "닉네임", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Text(text = "댓글작성", color = Color.LightGray, fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                modifier = Modifier.align(Alignment.End).size(width = 40.dp, height = 20.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFC5E1A5)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "등록", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 🧩 댓글 아이템 컴포넌트
@Composable
fun CommentItem(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(modifier = Modifier.size(20.dp), shape = CircleShape, color = Color.LightGray) {}
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = comment.authorNickname, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = comment.content, fontSize = 12.sp)
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CommunityPostPreview() {
    CommunityPostScreen(navController = rememberNavController())
}
