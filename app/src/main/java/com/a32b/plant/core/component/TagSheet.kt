package com.a32b.plant.core.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.a32b.plant.data.model.Tag
import com.a32b.plant.ui.theme.Typography
import com.a32b.plant.ui.theme.primary

@Composable
fun TagSheet(tags: List<Tag>, init : List<Tag> = emptyList(),
             isMultiSelected: Boolean = false,
             enable: Boolean = true,
             onSelectedChange: (List<Tag>) -> Unit) {
    val selectedTags = remember { mutableStateListOf<Tag>() }
    LaunchedEffect(init) {
        selectedTags.clear()
        selectedTags.addAll(init)
    }

    val groupedTags = tags.groupBy { it.parentId }

    Card(modifier = Modifier.padding(10.dp).fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            groupedTags.forEach { (parent, group) ->
                Text(parent, style = Typography.titleSmall, modifier = Modifier.padding(start = 10.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer)

                LazyRow(modifier = Modifier.padding(start = 10.dp)) {
                    items(group) { tag ->
                        val isLocked = !enable && init.contains(tag)
                        Card(shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTags.contains(tag)) primary else MaterialTheme.colorScheme.secondaryContainer,
                                disabledContainerColor = if(selectedTags.contains(tag)) primary else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp,
                                disabledElevation = 2.dp
                            ),
                            modifier = Modifier.padding(2.dp),
                            enabled = !isLocked,
                            onClick = {
                                if (isMultiSelected){
                                    selectedTags.toggle(tag)
                                } else{
                                    selectedTags.clear()
                                    selectedTags.add(tag)
                                }
                                onSelectedChange(selectedTags)
                            }) {
                            Text(tag.name, modifier = Modifier.padding(top = 5.dp, bottom = 5.dp, start = 10.dp, end = 10.dp),
                                style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                    }
                }
            }
        }
    }




}

fun SnapshotStateList<Tag>.toggle(tag: Tag) {
    if (contains(tag)) remove(tag) else add(tag)
}
