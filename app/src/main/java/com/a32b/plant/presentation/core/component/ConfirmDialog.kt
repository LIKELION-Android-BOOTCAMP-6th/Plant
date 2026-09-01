package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.presentation.theme.primary
import com.a32b.plant.presentation.theme.sub2

@Composable
fun ConfirmDialog(
    text: String,
    semiText: String? = null,
    isSingleBtn: Boolean = false,
    isCancelable: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = {if (isCancelable) onDismiss() }) {
        Card(shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(3.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(22.dp)) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(text, style = MaterialTheme.typography.titleSmall)
                semiText?.let {
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(semiText, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    if (!isSingleBtn){
                        Button(onClick = onDismiss,
                            modifier = Modifier.height(30.dp).weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(sub2)) {
                            Text("취소", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Button(onClick = onConfirm,
                        modifier = Modifier.height(30.dp).weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(primary)) {
                        Text("예", style = MaterialTheme.typography.bodyMedium)
                    }
                }

            }
        }
    }
}
