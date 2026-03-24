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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun NewBornTree(navController: NavController,
                viewModel: NewBornTreeViewModel = viewModel()
){
    val context = LocalContext.current
    val tags by viewModel.dbTags.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    var selectedTag by remember { mutableStateOf("") }
    var potName by remember {mutableStateOf("")}
    //실패
    LaunchedEffect(Unit) {
        viewModel.errorMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            Text(
                text = "Plant",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.displayLarge
            )
          },
        bottomBar = {
            // 하단 버튼
            Button(
                onClick = {
                    viewModel.createPot(selectedTag, potName){
                        navController.popBackStack()
                    }
                },
                enabled = checkEnable(selectedTag,potName,isUploading),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(sub1),
                shape = RoundedCornerShape(16.dp)
            ) {
                if(isUploading){
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = background)
                }else{
                    Text("완료", style = MaterialTheme.typography.titleSmall, color = fontColor)
                }
            }
        }
      ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background),
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
                        .background(primary),
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
        }
    }
}
fun checkEnable(selectedTag:String, potName:String, isUploading: Boolean): Boolean{
    if(selectedTag.isNotEmpty() && potName.isNotEmpty() && !isUploading){
        return true
    }
    return false

}