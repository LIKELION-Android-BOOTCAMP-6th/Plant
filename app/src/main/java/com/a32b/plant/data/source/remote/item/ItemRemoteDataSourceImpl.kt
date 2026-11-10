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

    override fun observeBoxCount(uid: String): Flow<Int> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.getLong("item.box")?.toInt() ?: 0)
            }
        awaitClose { listener.remove() }
    }
}
