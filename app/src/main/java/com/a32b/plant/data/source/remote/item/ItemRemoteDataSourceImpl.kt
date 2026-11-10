package com.a32b.plant.data.source.remote.item

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRemoteDataSourceImpl @Inject constructor(
    private val db : FirebaseFirestore,
    private val functions: FirebaseFunctions
): ItemRemoteDataSource {

    override suspend fun openBonusBoxes(boxCount: Int, requestId: String): BonusBoxOpeningDto {
        val result = functions.getHttpsCallable("openBonusBoxes")
            .call(mapOf("count" to boxCount, "requestId" to requestId))
            .await()
        val items = (result.getData() as? Map<*, *>)?.get("items") as? List<*>
        return BonusBoxOpeningDto(items = items.orEmpty().filterIsInstance<String>())
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
}
