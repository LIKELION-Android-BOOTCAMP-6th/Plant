package com.a32b.plant.presentation.splash

import androidx.compose.runtime.Composable
import com.a32b.plant.presentation.core.component.ConfirmDialog

/**
 * 최소 지원 버전보다 낮을 때 보여주는 업데이트 안내 (#662)
 * - 버튼은 "업데이트" 하나. 눌러도 닫히지 않는다 (스토어에서 돌아와도 계속 보임)
 * - 바깥 터치로 닫히지 않고, 뒤로가기는 [onBack](앱 종료)으로 보낸다
 */
@Composable
fun UpdateRequiredDialog(
    message: String?,
    onUpdate: () -> Unit,
    onBack: () -> Unit
) {
    ConfirmDialog(
        text = "새 버전으로 업데이트해 주세요",
        semiText = message ?: DEFAULT_UPDATE_MESSAGE,
        isSingleBtn = true,
        confirmBtnText = "업데이트",
        dismissOnClickOutside = false,
        onDismiss = onBack,
        onConfirm = onUpdate
    )
}

private const val DEFAULT_UPDATE_MESSAGE = "지금 버전에서는 앱을 사용할 수 없어요.\n업데이트한 뒤 다시 실행해 주세요."
