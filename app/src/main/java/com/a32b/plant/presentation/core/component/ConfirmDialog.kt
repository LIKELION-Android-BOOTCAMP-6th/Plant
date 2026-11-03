package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
    buttonTextStyle: TextStyle? = null
) {
    Dialog(onDismissRequest = {if (isCancelable) onDismiss() }) {
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
                    Text(text, style = textStyle ?: MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
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
                        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)) {
                        Text(confirmBtnText, style = buttonTextStyle ?: MaterialTheme.typography.bodyMedium)
                    }
                }

            }
        }
    }
}
