//package com.a32b.plant.ui.feature.home.ui
//
//import android.widget.Toast
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.ButtonDefaults
//import androidx.compose.material3.CircularProgressIndicator
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import com.a32b.plant.ui.feature.home.viewmodel.NewBornTreeViewModel
//import com.a32b.plant.ui.theme.sub1
//
//@Composable
//fun NewBornTree(navController: NavController,
//                viewModel: NewBornTreeViewModel = viewModel()
//){
//    val tags by viewModel.dbTags.collectAsState()
//    val isUploading by viewModel.isUploading.collectAsState()
//
//    val selectedTag by remember { mutableStateOf("") }
//    val potName by remember {mutableStateOf("")}
//
//    Scaffold(
//        bottomBar = {
//            // 하단 버튼
////            Button(
////                onClick = { viewModel.createPot(selectedTag, potName){ navController.popBackStack()} },
////
////                enabled = selectedTag.isNotEmpty() && potName.isNotEmpty() && !isUploading,
////                modifier = Modifier.fillMaxWidth().padding(20.dp).height(60.dp),
////                colors = ButtonDefaults.buttonColors(sub1)
////            )
//        }
//    ) { }
//}