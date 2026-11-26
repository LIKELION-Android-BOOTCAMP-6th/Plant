package com.a32b.plant.presentation.studying.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.a32b.plant.R
import com.a32b.plant.core.extension.isCompletedAt
import com.a32b.plant.core.navigation.rememberPopBackStackSafely
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.presentation.studying.viewmodel.StudyRewardUi
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.presentation.core.component.ConfettiOverlay
import com.a32b.plant.presentation.core.component.ConfirmDialog
import com.a32b.plant.presentation.core.component.LoadableScreen
import com.a32b.plant.presentation.core.component.LoadingDialog
import com.a32b.plant.presentation.core.component.ProfileImage
import com.a32b.plant.presentation.core.component.StudyGoalItem
import com.a32b.plant.presentation.core.extension.resId
import com.a32b.plant.presentation.core.extension.showToast
import com.a32b.plant.presentation.studying.viewmodel.StudyResultEvent
import com.a32b.plant.presentation.studying.viewmodel.StudyResultViewModel

@Composable
fun StudyResultScreen(navController: NavController, viewModel: StudyResultViewModel = hiltViewModel()) {
    val popBackStackSafely = rememberPopBackStackSafely(navController)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val capture = captureBitmap {
        StudyResultContent(uiState.timestamp, uiState.tag, uiState.title, uiState.log, uiState.completed, uiState.level, uiState.time, reward = null, scrollable = false)
    }
    var showConfetti by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is StudyResultEvent.ShowToast -> context.showToast(event.message)
            }
        }
    }

    LoadableScreen(viewModel) {
        Box(modifier = Modifier.fillMaxSize()){
            Surface(modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { popBackStackSafely() }) {
                            Image(painter = painterResource(R.drawable.ic_study_result_close),
                                contentDescription = "닫기",
                                modifier = Modifier.size(24.dp))
                        }
                        // 갤러리 저장을 지원하지 않는 기기(Android 9 이하)에서는 저장 버튼 숨김
                        if (uiState.isImageSaveSupported) {
                            IconButton(onClick = {
                                viewModel.onDialogShow()
                            }) {
                                Image(painter = painterResource(R.drawable.ic_study_result_download),
                                    contentDescription = "이미지 저장하기",
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    StudyResultContent(uiState.timestamp, uiState.tag, uiState.title, uiState.log, uiState.completed, uiState.level, uiState.time, uiState.reward)

                }

            }

            if (showConfetti) ConfettiOverlay(onFinished = {showConfetti = false})
            if (uiState.isLoading) LoadingDialog()

        }
    }


    if (uiState.isDialogShow){
        ConfirmDialog("결과를 이미지로 저장하시겠습니까?",
            onDismiss = {viewModel.onDialogDismiss()},
            onConfirm = {
                viewModel.saveBitmapToGallery(capture.invoke())
                viewModel.onDialogDismiss()
            })

    }
}

@Composable
fun StudyResultContent(
    timestamp: String,
    tag: String,
    title: String,
    log: List<String>,
    completed: List<Boolean>,
    level: String,
    time: Long,
    reward: StudyRewardUi?,
    scrollable: Boolean = true
) {
    val screenModifier = if (scrollable) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
    val scrollState = rememberScrollState()

    Surface(modifier = screenModifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = screenModifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(17.dp))

            StudyTimestampText(timestamp)
            Spacer(modifier = Modifier.height(13.dp))

            val cardModifier = Modifier.fillMaxWidth()
                .then(if (scrollable) Modifier.weight(1f) else Modifier)
                .padding(7.dp)

            Card(modifier = cardModifier,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)

            ) {
                val contentModifier = Modifier.fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)

                Column(modifier = contentModifier,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(22.dp))
                    // 부모 Column이 가운데 정렬이라 너비를 채워야 길이와 관계없이 좌측 정렬됨
                    Text(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp),
                        text = "[$tag] $title", style = MaterialTheme.typography.titleSmall,)

                    log.forEachIndexed { index, text ->
                        StudyGoalItem(
                            text = text,
                            isCompleted = completed.isCompletedAt(index),
                            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    // 화분 이미지는 Lv6까지 담느라 화분 폭이 영역의 약 41%라서, 영역을 키워 화분이 작아 보이지 않게 함
                    ProfileImage(level, 200)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 카드 안쪽 폭보다 길면 한 줄에 들어가는 크기까지만 줄임 (최대 displayMedium 크기, #673)
                    val timerStyle = MaterialTheme.typography.displayMedium
                    Text(
                        TimeFormatter.formatToDigitalClock(time),
                        style = timerStyle,
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = timerStyle.fontSize)
                    )

                    when (reward) {
                        is StudyRewardUi.Granted -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            StudyRewardSection(reward.reward)
                        }
                        StudyRewardUi.Loading -> RewardNotice("보상 받는 중…")
                        StudyRewardUi.Delayed -> RewardNotice("보상 지급이 늦어지고 있어요.\n지급되면 홈에서 알려드릴게요.")
                        StudyRewardUi.WaitingForNetwork -> RewardNotice("온라인에 연결되면 보상이 지급돼요.")
                        StudyRewardUi.Hidden, null -> Unit
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("학습 시간이 식물에게 전해졌어요!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(22.dp))

                }

            }

        }

    }

}

/**
 * 결과 화면 상단 날짜·시각 (예: "2026년 11월 22일 일요일 09:51 ~ 09:54")
 *
 * 한 줄에 안 들어가면 16sp까지 줄여 한 줄로 맞추고, 16sp로도 안 들어가면 원래 크기(22sp)로 줄바꿈한다 (#673)
 * 한 줄 고정만 쓰면 360dp 폭·글꼴 2.0에서 끝("~ 09:54")이 잘림 — 줄바꿈은 잘리지 않으므로 마지막 수단으로 둠
 * 16sp 한 줄이 들어가는지는 현재 폭에서 재서 정함 (폭이 바뀌면 다시 잼)
 */
@Composable
private fun StudyTimestampText(timestamp: String) {
    val style = MaterialTheme.typography.headlineSmall
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fitsOneLine = !textMeasurer.measure(
            text = timestamp,
            style = style.copy(fontSize = TIMESTAMP_MIN_FONT_SIZE),
            maxLines = 1,
            softWrap = false,
            constraints = Constraints(maxWidth = constraints.maxWidth)
        ).hasVisualOverflow
        Text(
            text = timestamp,
            style = style,
            textAlign = TextAlign.Center,
            maxLines = if (fitsOneLine) 1 else Int.MAX_VALUE,
            autoSize = if (fitsOneLine) {
                TextAutoSize.StepBased(minFontSize = TIMESTAMP_MIN_FONT_SIZE, maxFontSize = style.fontSize)
            } else null
        )
    }
}

// 줄일 수 있는 최소 크기. 최대 크기는 headlineSmall(22sp)
private val TIMESTAMP_MIN_FONT_SIZE = 16.sp

/** 보상이 아직 표시되지 않을 때의 안내 (보상은 서버가 지급, #453) */
@Composable
private fun RewardNotice(text: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun StudyRewardSection(reward: StudyReward) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "획득 보상",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                reward.items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = item.resId),
                            contentDescription = item.kor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "${item.kor} ×1",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (reward.boxCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = ItemType.BOX.resId),
                            contentDescription = "보너스 박스",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "보너스 박스 ×${reward.boxCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun captureBitmap(content: @Composable () -> Unit): () -> Bitmap{
    val context = LocalContext.current
    val composeView = remember { ComposeView(context) }

    fun captureBitmap(): Bitmap {
        val width = composeView.width.takeIf { it > 0 }
            ?: context.findActivity()?.window?.decorView?.width?.takeIf { it > 0 }
            ?: context.resources.displayMetrics.widthPixels
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        // 화면 높이로 측정하면 긴 로그가 잘려서 캡처되므로, 높이는 무제한으로 재측정해 전체 콘텐츠를 담는다
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        composeView.measure(widthSpec, heightSpec)
        composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

        val bitmap = Bitmap.createBitmap(composeView.measuredWidth, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
        composeView.draw(Canvas(bitmap))
        return bitmap
    }

    AndroidView(
        factory = {composeView.apply { setContent { content.invoke() } }}
    )

    return ::captureBitmap
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
