package com.a32b.plant.presentation.community.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityViewModel
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.a32b.plant.R
import com.a32b.plant.presentation.core.component.TagGroup
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.presentation.community.viewmodel.CommunityActivityEvent
@Composable
fun CommunityActivityScreen(navController: NavController, viewModel: CommunityActivityViewModel = hiltViewModel()){

    val uiState by viewModel.uiState.collectAsState()
    val list = listOf(ActivityType.POST, ActivityType.COMMENT, ActivityType.LIKE)

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CommunityActivityEvent.NavigateToCommunityDetail -> {
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
                Text("내 활동", style = MaterialTheme.typography.titleLarge)
            }

            TagGroup(list, init = listOf(uiState.selected),isMultiSelected = false){ selected ->
                viewModel.onSelectedChange(selected.get(0))
            }

            if (uiState.activities.isEmpty()){
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    Text(
                        text = "커뮤니티 활동 내역이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }else{
                ContentList(uiState.activities){ targetId ->
                    Log.d("타겟 아이디", targetId)
                    viewModel.moveToCommunityDetail(targetId)
                }
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
                        Text(list.title, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(TimeFormatter.formatTimeToDate(list.createAt ?: 0), style = MaterialTheme.typography.bodySmall)
                    }
                    list.comment?.let {
                        Text(
                            list.comment,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
        }
    }
}


