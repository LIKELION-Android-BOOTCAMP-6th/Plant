package com.a32b.plant.data.source.remote.item

import com.a32b.plant.domain.type.ItemType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRemoteDataSourceImpl @Inject constructor(
    private val db : FirebaseFirestore
): ItemRemoteDataSource {

    override suspend fun addDrawItems(uid: String, items: List<ItemType>) {
        val updates = buildUpdates(items)

        if (updates.isEmpty()) return

        db.collection("users").document(uid)
            .update(updates)
            .await()
    }

    override suspend fun openBonusBoxes(uid: String, items: List<ItemType>) {
        val ref = db.collection("users").document(uid)
        val boxCount = items.size.toLong()

        db.runTransaction {
            val box = it.get(ref).getLong("item.box") ?: 0
            check( box >= boxCount) {"보너스 박스의 개수가 부족합니다."}

            it.update(ref, buildUpdates(items) + ("item.box" to FieldValue.increment(-boxCount)))
        }.await()
    }

    private fun buildUpdates(items: List<ItemType>) : Map<String, Any>{
        val updates = mutableMapOf<String, Any>()

        val (coinItems, normalItems) = items.partition { it.fieldKey == ItemType.GOLD_300.fieldKey }

        normalItems
            .groupingBy { it.fieldKey }
            .eachCount()
            .forEach { (key, count) ->
                updates["item.$key"] = FieldValue.increment(count.toLong())
            }

        val totalCoin = coinItems.sumOf { it.price }
        if (totalCoin > 0) {
            updates[ItemType.GOLD_300.fieldKey] = FieldValue.increment(totalCoin.toLong())
        }

        return updates
    }
}
