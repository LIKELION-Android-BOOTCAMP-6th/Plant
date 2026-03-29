package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.a32b.plant.R
import com.a32b.plant.core.component.TagGroup
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background

@Composable
//fun MyCommunityFeedScreen(navController: NavController) {
fun MyCommunityFeedScreen() {
//    val viewModel : MyCommunityFeedViewModel = viewModel(factory = ViewModelFactory.myCommunityFeedViewModelFactory)

    val list = remember { mutableStateListOf<String>()}
    list.add("내 게시물")
    list.add("내 댓글")
    list.add("좋아요")

    var select by remember { mutableStateOf("내 게시물") }


    val postList : List<CommunityActivity> = listOf(
        CommunityActivity(type = "내 게시물", title = "게시물1", targetId = "", createAt = "2026.01.01"),
        CommunityActivity(type = "내 게시물", title = "게시물2", targetId = "", createAt = "2026.01.01"),
        CommunityActivity(type = "내 게시물", title = "게시물3", targetId = "", createAt = "2026.01.01")
    )
    val commentList: List<CommunityActivity> = listOf(
        CommunityActivity(type = "내 댓글", title = "게시물1", targetId = "", comment = "eotrmf", createAt = "2026.01.01"),
        CommunityActivity(type = "내 댓글", title = "게시물1", targetId = "", comment = "d", createAt = "2026.01.01"),
        CommunityActivity(type = "내 댓글", title = "게시물1", targetId = "", comment = "daa", createAt = "2026.01.01")
    )


    Surface(modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
            Box(modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center){
//                IconButton(onClick = {navController.popBackStack()}) {
                IconButton(onClick = {},
                    modifier = Modifier.size(30.dp).align(Alignment.CenterStart)) {
                    Image(painter = painterResource(R.drawable.ic_backbtn),
                        contentDescription = "뒤로가기")
                }
                Text("커뮤니티 활동", style = Typography.titleLarge)
            }

            TagGroup(list, false){ selected ->
                Log.d("선택된 거", selected.toString())
                select = selected.get(0)
            }
            when(select){
                "내 게시물" -> ContentList(postList)
                "내 댓글" -> ContentList(commentList)
                "좋아요" -> ContentList(postList)
            }

        }
    }
}

@Composable
fun ContentList(lists : List<CommunityActivity>){

    lists.forEach { list ->
        Card(modifier = Modifier.fillMaxWidth().padding(10.dp),
            shape = RoundedCornerShape(7.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {Column(modifier = Modifier.padding(top = 13.dp, bottom = 13.dp, start = 7.dp, end = 7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(list.title, style = Typography.titleSmall)
                Text(list.createAt, style = Typography.bodySmall)
            }
            list.comment?.let {
                Text(list.comment, style = Typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
            }
        }


    }



    }


}

@Preview
@Composable
fun MyCommunityFeedScreenPreview(){
    MyCommunityFeedScreen()
}