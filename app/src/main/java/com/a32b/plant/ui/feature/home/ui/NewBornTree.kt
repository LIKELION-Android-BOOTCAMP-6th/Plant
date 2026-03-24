package com.a32b.plant.ui.feature.home.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.a32b.plant.ui.feature.home.viewmodel.NewBornTreeViewModel
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.sub1

@Composable
fun NewBornTree(navController: NavController,
                viewModel: NewBornTreeViewModel = viewModel()
){
    val tags by viewModel.dbTags.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()

    val selectedTag by remember { mutableStateOf("") }
    val potName by remember {mutableStateOf("")}

    Scaffold(
        topBar = {
            Text(
                text = "Plant",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.displayLarge
            )
          }
      ){ paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 화분 기본 이미지
          item{

          }

        bottomBar = {
             하단 버튼
            Button(
                onClick = { viewModel.createPot(selectedTag, potName){ navController.popBackStack()} },

                enabled = selectedTag.isNotEmpty() && potName.isNotEmpty() && !isUploading,
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(60.dp),
                colors = ButtonDefaults.buttonColors(sub1)
            )
        }
    ) {

          }
}