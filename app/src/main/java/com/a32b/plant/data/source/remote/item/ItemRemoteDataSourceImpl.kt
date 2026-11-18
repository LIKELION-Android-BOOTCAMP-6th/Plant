package com.a32b.plant.data.source.remote.item

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRemoteDataSourceImpl @Inject constructor(
    private val functions: FirebaseFunctions
): ItemRemoteDataSource {

    override suspend fun openBonusBoxes(boxCount: Int, requestId: String): BonusBoxOpeningDto {
        val result = functions.getHttpsCallable("openBonusBoxes")
            .call(mapOf("count" to boxCount, "requestId" to requestId))
            .await()
        val items = (result.getData() as? Map<*, *>)?.get("items") as? List<*>
        return BonusBoxOpeningDto(items = items.orEmpty().filterIsInstance<String>())
    }
}
