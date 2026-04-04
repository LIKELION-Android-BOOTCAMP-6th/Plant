package com.a32b.plant.ui.feature.studying.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.drawToBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.R
import com.a32b.plant.core.component.ConfirmDialog
import com.a32b.plant.core.component.ProfileImage
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.ViewModelFactory
import com.a32b.plant.ui.feature.studying.viewmodel.StudyResultViewModel
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.background
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub2

@Composable
fun StudyResultScreen(navController: NavController) {
    val args = navController.currentBackStackEntry?.toRoute<Routes.StudyResult>()

    val timestamp = args!!.timestamp
    val tag = args.tag
    val title = args.title
    val log = args.log
    val level = args.level
    val time = args.time

    val viewModel: StudyResultViewModel = viewModel(factory = ViewModelFactory.studyResultViewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val capture = CaptureBitmap {
        StudyResultContent(timestamp, tag, title, log, level, time)
    }
    Surface(modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {navController.popBackStack()}) {
                    Image(painter = painterResource(R.drawable.ic_study_result_close),
                        contentDescription = "닫기")
                }
                IconButton(onClick = {
                    viewModel.onDialogShow()
                }) {
                    Image(painter = painterResource(R.drawable.ic_study_result_download),
                        contentDescription = "이미지 저장하기")
                }
            }

            StudyResultContent(timestamp, tag, title, log, level, time)
        }
    }

    if (uiState.isDialogShow){
        ConfirmDialog("결과를 이미지로 저장하시겠습니까?",
            onDismiss = {viewModel.onDialogDismiss()},
            onConfirm = {
                viewModel.saveBitmapToGallery(context, capture.invoke())
                Toast.makeText(context,"저장이 완료됐습니다!", Toast.LENGTH_SHORT).show()
                viewModel.onDialogDismiss()
            })

    }
}

@Composable
fun StudyResultContent(timestamp: String, tag: String, title: String, log: List<String>, level: String, time: Long){
    Surface(modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(17.dp))

            Text(timestamp , style = Typography.titleSmall , fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(13.dp))

            Card(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(7.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)

            ) {
                Spacer(modifier = Modifier.height(22.dp))
                Text(modifier = Modifier.padding(start = 12.dp),
                    text = "[$tag] $title", style = Typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface)

                log.forEach { text ->
                    Text("- $text", style = Typography.bodyMedium, modifier = Modifier.padding(start = 22.dp, top = 10.dp, bottom = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface)
                }

                Column(modifier = Modifier.fillMaxWidth()
                    ,horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileImage(level, 200)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(TimeFormatter.formatToDigitalClock(time), style = Typography.titleLarge, fontSize = 50.sp,
                        color = MaterialTheme.colorScheme.onSurface)

                    Spacer(modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painter = painterResource(id = R.drawable.logo_plant),
                            contentDescription = "logo",
                            modifier = Modifier.size(30.dp).padding(end = 10.dp))
                        Text("식물이 한 뼘 더 자랐습니다!", style = Typography.bodySmall, color = MaterialTheme.colorScheme.primary)

                    }
                    Spacer(modifier = Modifier.height(22.dp))

                }

            }

        }

    }

}

@Composable
fun CaptureBitmap(content: @Composable () -> Unit): () -> Bitmap{
    val context = LocalContext.current
    val composeView = remember { ComposeView(context) }

    fun captureBitmap(): Bitmap = composeView.drawToBitmap()

    AndroidView(
        factory = {composeView.apply { setContent { content.invoke() } }}
    )

    return ::captureBitmap
}