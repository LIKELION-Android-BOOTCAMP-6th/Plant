package com.a32b.plant.data.source.remote.item

import com.a32b.plant.domain.type.ItemType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    override suspend fun openBonusBoxes(uid: String, boxCount: Int, items: List<ItemType>) {
        val ref = db.collection("users").document(uid)
        val neededBoxes = boxCount.toLong()

        db.runTransaction { transaction ->
            val box = transaction.get(ref).getLong("item.box") ?: 0
            check( box >= neededBoxes) {"보너스 박스의 개수가 부족합니다."}

            transaction.update(ref, buildUpdates(items) + ("item.box" to FieldValue.increment(-neededBoxes)))
        }.await()
    }

    override suspend fun consumeItems(uid: String, items: Map<ItemType, Int>) {
        if (items.isEmpty()) return
        val ref = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(ref)

            for ((itemType, required) in items) {
                val current = snapshot.getLong("item.${itemType.fieldKey}")?.toInt() ?: 0
                check(current >= required) { "${itemType.kor}이(가) 부족합니다." }
            }

            val decrements = items.entries.associate { (itemType, required) ->
                "item.${itemType.fieldKey}" to FieldValue.increment(-required.toLong())
            }
            transaction.update(ref, decrements)
        }.await()
    }

    override suspend fun canConsumeItems(uid: String, items: Map<ItemType, Int>): Boolean {
        if (items.isEmpty()) return true
        val snapshot = db.collection("users").document(uid).get().await()

        return items.all { (itemType, required) ->
            val current = snapshot.getLong("item.${itemType.fieldKey}")?.toInt() ?: 0
            current >= required
        }
    }

    override fun observeBoxCount(uid: String): Flow<Int> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.getLong("item.box")?.toInt() ?: 0)
            }
        awaitClose { listener.remove() }
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
