package com.a32b.plant.core.component

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.primary

@Composable
fun TagGroup(tags: List<String>, init: List<String> = emptyList(),
             isMultiSelected: Boolean = true,
             enable: Boolean = true,
             editing: Boolean = false,
             onSelectedChanged: (List<String>) -> Unit = {}) {
    val context = LocalContext.current
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
                    containerColor = if (selectedTags.contains(tag)) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = if (selectedTags.contains(tag)) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp,
                    disabledElevation = 3.dp),
                modifier = Modifier.padding(5.dp),
                enabled = enable,
                onClick = {
                    if (isMultiSelected) {
                        //다중 선택
                        if (editing) {
                            val blockedIndexes = listOf(0, 1, 2)
                            val clickedIndex = tags.indexOf(tag)
                            val isConflicted = clickedIndex in blockedIndexes &&
                                    blockedIndexes.filter { it != clickedIndex } //선택하지 않은 걸 골라내고
                                        .any { tags.getOrNull(it) in selectedTags } // 그것들이 셀렉티드에 있는지 확인

                            if (isConflicted)
                                Toast.makeText(context, "학생 태그는 하나만 선택할 수 있습니다!", Toast.LENGTH_SHORT).show()
                            else selectedTags.toggle(tag)
                        } else {
                            selectedTags.toggle(tag)
                        }
                    } else {
                        //단일 선택
                        selectedTags.clear()
                        selectedTags.add(tag)
                    }
                    onSelectedChanged(selectedTags)
                }
            ) {
                Text(tag, Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp), style = Typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
fun SnapshotStateList<String>.toggle(tag: String) {
    if (contains(tag)) remove(tag) else add(tag)
}