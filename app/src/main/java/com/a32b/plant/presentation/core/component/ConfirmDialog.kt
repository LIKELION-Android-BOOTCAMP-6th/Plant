package com.a32b.plant.presentation.core.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.a32b.plant.R
import com.a32b.plant.presentation.theme.PlantTheme

private const val DIALOG_CONTENT_MAX_HEIGHT_RATIO = 0.5f

@Composable
fun ConfirmDialog(
    text: String,
    semiText: String? = null,
    isSingleBtn: Boolean = false,
    isCancelable: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmBtnText: String = "예",
    textStyle: TextStyle? = null,
    buttonTextStyle: TextStyle? = null,
    isLoading: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    isDestructive: Boolean = false,
    @DrawableRes iconRes: Int? = null
) {
    // iconRes: 본문(text) 왼쪽에 24dp 아이콘을 붙인다. 출석 보상 알림처럼 무엇을 받았는지 보여줄 때 사용 (#681)
    // isLoading: 확인 후 처리 중 — 버튼을 막고 로딩 표시, 바깥 터치·뒤로가기로 닫지 않음
    // isDestructive: 삭제·탈퇴처럼 되돌릴 수 없는 동작 — 확인 버튼을 error 배경 + onError 글자로 강조 (#647). confirmBtnText에 동작 이름("삭제", "탈퇴")을 함께 넘길 것
    // dismissOnClickOutside: false면 바깥 터치는 무시한다. 뒤로가기는 isCancelable이고 로딩 중이 아닐 때 onDismiss로 보냄 (업데이트 안내의 뒤로가기 = 앱 종료, #662)
    Dialog(
        onDismissRequest = { if (isCancelable && !isLoading) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = dismissOnClickOutside)
    ) {
        // 다이얼로그는 별도 창이라 화면의 LoadingBox가 위에 그려지지 않으므로 다이얼로그 안에 겹쳐 표시
        Box {
            Card(shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(3.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(22.dp)) {
                    Spacer(modifier = Modifier.height(10.dp))

                    val maxContentHeight = LocalConfiguration.current.screenHeightDp.dp * DIALOG_CONTENT_MAX_HEIGHT_RATIO
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .heightIn(max = maxContentHeight)
                            .verticalScroll(rememberScrollState())) {
                        if (iconRes != null) {
                            // 글자가 길거나 글꼴이 커도 아이콘이 밀려나지 않도록 글자에 남은 폭만 주고 줄바꿈시킨다
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = textStyle ?: MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(text, style = textStyle ?: MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        }
                        semiText?.let {
                            Spacer(modifier = Modifier.height(9.dp))
                            Text(semiText, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (!isSingleBtn){
                            Button(onClick = onDismiss,
                                modifier = Modifier.heightIn(min = 30.dp).weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.outline)) {
                                Text("취소", style = buttonTextStyle ?: MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Button(onClick = onConfirm,
                            modifier = Modifier.heightIn(min = 30.dp).weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isDestructive) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
                            }) {
                            // Type.kt 스타일에 글자색(onSurface)이 들어 있어 contentColor만으로는 바뀌지 않으므로 color로 덮어씀. 일반 버튼은 스타일 색 그대로
                            Text(confirmBtnText,
                                style = buttonTextStyle ?: MaterialTheme.typography.bodyMedium,
                                color = if (isDestructive) MaterialTheme.colorScheme.onError else Color.Unspecified)
                        }
                    }

                }
            }
            if (isLoading) {
                LoadingBox(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(30.dp))
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "위험 동작 - 라이트")
@Composable
private fun ConfirmDialogDestructiveLightPreview() {
    PlantTheme(darkTheme = false) {
        ConfirmDialog(
            text = "기록 삭제",
            semiText = "정말로 이 학습 기록을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
            onDismiss = {},
            onConfirm = {},
            confirmBtnText = "삭제",
            isDestructive = true
        )
    }
}

@Preview(showBackground = true, name = "위험 동작 - 다크")
@Composable
private fun ConfirmDialogDestructiveDarkPreview() {
    PlantTheme(darkTheme = true) {
        ConfirmDialog(
            text = "정말로 탈퇴하시겠습니까?",
            semiText = "탈퇴 시 모든 학습 기록이 삭제되며 복구할 수 없습니다.",
            onDismiss = {},
            onConfirm = {},
            confirmBtnText = "탈퇴",
            isDestructive = true
        )
    }
}

@Preview(showBackground = true, name = "일반 확인 - 다크")
@Composable
private fun ConfirmDialogDefaultDarkPreview() {
    PlantTheme(darkTheme = true) {
        ConfirmDialog(text = "로그아웃 하시겠습니까?", onDismiss = {}, onConfirm = {})
    }
}

@Preview(showBackground = true, name = "아이콘 - 라이트")
@Composable
private fun ConfirmDialogIconLightPreview() {
    PlantTheme(darkTheme = false) {
        ConfirmDialog(
            text = "100골드를 획득했습니다!",
            isSingleBtn = true,
            onDismiss = {},
            onConfirm = {},
            iconRes = R.drawable.ic_coin
        )
    }
}

@Preview(showBackground = true, name = "아이콘 - 다크")
@Composable
private fun ConfirmDialogIconDarkPreview() {
    PlantTheme(darkTheme = true) {
        ConfirmDialog(
            text = "비료 1개를 획득했습니다!",
            isSingleBtn = true,
            onDismiss = {},
            onConfirm = {},
            iconRes = R.drawable.ic_fertilizer
        )
    }
}
