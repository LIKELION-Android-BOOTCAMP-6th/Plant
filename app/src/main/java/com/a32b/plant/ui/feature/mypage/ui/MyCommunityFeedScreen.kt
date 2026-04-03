package com.a32b.plant.ui.feature.mypage.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.component.TagGroup
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.ActivityType
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.ui.feature.mypage.viewmodel.MyCommunityFeedEvent
import com.a32b.plant.ui.feature.mypage.viewmodel.MyCommunityFeedViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background

@Composable
fun MyCommunityFeedScreen(navController: NavController) {
    val viewModel : MyCommunityFeedViewModel = viewModel(factory = ViewModelFactory.myCommunityFeedViewModelFactory)

    val uiState by viewModel.uiState.collectAsState()
    val list = listOf(ActivityType.POST, ActivityType.COMMENT, ActivityType.LIKE)

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is MyCommunityFeedEvent.NavigateToCommunityDetail -> {
                    navController.navigate(Routes.CommunityDetail(event.postId ))
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(15.dp)) {
            Box(modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center){
                IconButton(onClick = {navController.popBackStack()},
                    modifier = Modifier.size(30.dp).align(Alignment.CenterStart)
                    ) {
                    Image(painter = painterResource(R.drawable.ic_backbtn),
                        contentDescription = "뒤로가기")
                }
                Text("내 활동", style = Typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            }

            TagGroup(list, init = listOf(uiState.selected),isMultiSelected = false){ selected ->
                viewModel.onSelectedChange(selected.get(0))
                Log.d("뷰모델 확니", uiState.selected)
            }

            ContentList(uiState.activities){ targetId ->
                Log.d("타겟 아이디", targetId)
                viewModel.moveToCommunityDetail(targetId)

            }
        }
    }
}

@Composable
fun ContentList(lists : List<CommunityActivity>, onClick: (String) -> Unit){

    LazyColumn {
        items(lists) { list->
            Card(
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                shape = RoundedCornerShape(7.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                onClick = {onClick(list.targetId)}
            ) {
                Column(
                    modifier = Modifier.padding(7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(list.title, style = Typography.titleSmall,maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(TimeFormatter.formatTimestamp(list.createAt), style = Typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    list.comment?.let {
                        Text(
                            list.comment,
                            style = Typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
