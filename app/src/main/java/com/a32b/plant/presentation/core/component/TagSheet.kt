package com.a32b.plant.presentation.core.component

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
import com.a32b.plant.domain.model.Tag

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
    val groupedTags = groupTagsByParent(tags)

    Card(modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth(),
        shape = RoundedCornerShape(5.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            groupedTags.forEach { (parent, group) ->
                Text(parent, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 10.dp))

                LazyRow(modifier = Modifier.padding(start = 10.dp)) {
                    items(group) { tag ->
                        val isLocked = !enable && init.contains(tag)
                        Card(shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTags.contains(tag)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                disabledContainerColor = if(selectedTags.contains(tag)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
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
                            Text(tag.name, modifier = Modifier.padding(top = 3.dp, bottom = 3.dp, start = 10.dp, end = 10.dp),
                                style = MaterialTheme.typography.bodySmall
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

// 상위 분류 표시 순서. Tags의 no는 분류마다 0부터 다시 매겨져 있어 분류끼리 순서를 정할 수 없다 (2026-11-24 운영 Tags 32개 기준, #650)
private val PARENT_ORDER = listOf("중등", "고등", "대학교", "취업준비", "자기계발")

/**
 * 태그를 상위 분류(parentId)별로 묶고 [PARENT_ORDER] 순서로 정렬한다.
 * 목록에 없는 분류는 맨 앞이 아니라 맨 뒤에 둔다 (#650). 그런 분류가 여럿이면 받은 목록에 먼저 나온 분류가 앞에 온다 (sortedBy는 안정 정렬).
 * 분류 안 태그 순서는 받은 목록 순서를 그대로 둔다.
 */
internal fun groupTagsByParent(tags: List<Tag>): List<Pair<String, List<Tag>>> =
    tags.groupBy { it.parentId }
        .map { (parent, group) -> parent to group }
        .sortedBy { (parent, _) -> PARENT_ORDER.indexOf(parent).takeIf { it >= 0 } ?: Int.MAX_VALUE }
