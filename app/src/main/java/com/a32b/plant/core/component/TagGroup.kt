package com.a32b.plant.core.component

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.primary

@Composable
fun TagGroup(tags: List<String>, init: List<String> = emptyList(),isMultiSelected: Boolean = true, enable: Boolean = true, onSelectedChanged: (List<String>) -> Unit = {}) {
    val selectedTags = remember { mutableStateListOf<String>() }
    LaunchedEffect(init) {
        selectedTags.clear()
        selectedTags.addAll(init)
    }
    LazyRow {
        items(tags) { tag ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTags.contains(tag)) primary else Color.White,
                    disabledContainerColor = if (selectedTags.contains(tag)) primary else Color.White),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp,
                    disabledElevation = 3.dp),
                modifier = Modifier.padding(5.dp),
                enabled = enable,
                onClick = {
                    if(isMultiSelected){
                        //다중 선택
                        if (selectedTags.contains(tag)) selectedTags.remove(tag)
                        else selectedTags.add(tag)
                    }else{
                        //단일 선택
                        selectedTags.clear()
                        selectedTags.add(tag)
                    }
                    onSelectedChanged(selectedTags)
                }
            ) {
                Text(tag, Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp), style = Typography.bodyMedium)
            }
        }
    }
}