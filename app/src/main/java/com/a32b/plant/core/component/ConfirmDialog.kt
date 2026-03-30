package com.a32b.plant.core.component

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub2

@Composable
fun ConfirmDialog(text: String, semiText:String? = null,onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Card(shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(22.dp)) {
                Spacer(modifier = Modifier.height(10.dp))

                Text(text, style = Typography.titleSmall)
                semiText?.let {
                    Spacer(modifier = Modifier.height(9.dp))
                    Text(semiText, style = Typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss,
                        modifier = Modifier.height(30.dp).weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(sub2)) {
                        Text("취소", style = Typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = onConfirm,
                        modifier = Modifier.height(30.dp).weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(primary)) {
                        Text("예", style = Typography.bodyMedium)
                    }
                }

            }
        }
    }

}