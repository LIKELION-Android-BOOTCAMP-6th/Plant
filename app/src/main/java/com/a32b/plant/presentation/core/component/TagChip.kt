package com.a32b.plant.presentation.core.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a32b.plant.presentation.theme.primary

@Composable
fun TagChip(text: String, size: Int, isSelected: Boolean = true) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) primary else MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.padding(5.dp),
    ) {
        Text(text, Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp), style = MaterialTheme.typography.bodyMedium, fontSize = size.sp)
    }
}
