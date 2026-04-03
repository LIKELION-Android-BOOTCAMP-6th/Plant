package com.a32b.plant.ui.feature.home.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.ui.feature.home.viewmodel.NewBornTreeViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.fontColor
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.a32b.plant.data.di.ViewModelFactory
import com.google.common.math.LinearTransformation.vertical

@Composable
fun NewBornTreeScreen(navController: NavController,
                      viewModel: NewBornTreeViewModel = viewModel(factory = ViewModelFactory.newBornTreeViewModelFactory)
){
    val context = LocalContext.current
    val tags by viewModel.dbTags.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var selectedTag by remember { mutableStateOf("") }
    var potName by remember {mutableStateOf("")}
    //실패
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            Text(
                text = "Plant",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
          },
        bottomBar = {
            // 하단 버튼
            Button(
                onClick = {
                    viewModel.createPot(selectedTag, potName.trim()){
                        navController.popBackStack()
                    }
                },
                enabled = checkEnable(selectedTag,potName,isUploading),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if(isUploading){
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                }else{
                    Text("완료", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
      ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // 화분 기본 이미지
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ){
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(R.drawable.ic_pot_lv0),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp)
                        )
                    }
                }
            }
            // 태그 출력
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text("태그 선택", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp))

                    //태그
                    androidx.compose.foundation.lazy.LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(tags.size){ index ->
                            val tag = tags[index]
                            androidx.compose.material3.FilterChip(
                                selected = (selectedTag == tag),
                                onClick = { selectedTag = tag},
                                label = {
                                    Text(tag, style = MaterialTheme.typography.bodySmall,color = MaterialTheme.colorScheme.onSurface)
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                // 선택 시 색상 변화
                                border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                    enabled = !isUploading,
                                    selected = (selectedTag == tag),
//                                    borderColor = androidx.compose.ui.graphics.Color.LightGray,
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 0.dp,
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                }
            }
            // 제목 입력
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("화분 이름",
                        style = MaterialTheme.typography.titleSmall,
                        color=MaterialTheme.colorScheme.onSurface)
                    androidx.compose.material3.OutlinedTextField(
                        value = potName,
                        onValueChange = { input ->
                            // 줄바꿈 금지 & 글자 수 제한
                            //공백 입력
                            if(!input.contains("\n") && input.length <= 15 && input.isEmpty() || input.isNotBlank()){
                                potName = input
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(
                            text="이름을 입력하세요 (최대 15글자)",
                                      color = MaterialTheme.colorScheme.onSurfaceVariant
                        )},

                        // 한 줄 입력 고정
                        singleLine = true,
                    )
                }

            }
        }
    }
}
fun checkEnable(selectedTag:String, potName:String, isUploading: Boolean): Boolean{
    if(selectedTag.isNotEmpty() && potName.trim().isNotEmpty() && !isUploading){
        return true
    }
    return false

}