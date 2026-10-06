package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.remote.item.ItemRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemRemoteDataSource: ItemRemoteDataSource
): ItemRepository {

    override suspend fun addDrawItems(uid: String, items: List<ItemType>): Result<Unit> = safeRunCatching {
        itemRemoteDataSource.addDrawItems(uid, items)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e -> Result.Failure(handleError(e, "알 수 없는 오류가 발생했습니다."))}
    )

    override suspend fun openBonusBoxes(uid: String, items: List<ItemType>): Result<Unit> = safeRunCatching {
        itemRemoteDataSource.openBonusBoxes(uid, items)
    }.fold(
        onSuccess = { Result.Success(Unit)},
        onFailure = { e -> Result.Failure(handleError(e, "알 수 없는 오류가 발생했습니다."))}
    )

    private fun handleError(e: Throwable, logMessage: String): AppError {
        if (e is CancellationException) throw e

        Log.e("ItemRepository", "$logMessage: ${e.message}", e)

        return when (e) {
            is FirebaseNetworkException -> AppError.Network()
            is IllegalStateException -> AppError.Custom(e.message ?: logMessage) //FirebaseFirestoreException 내부로 들어가진 않는지 확인 필요
            else -> AppError.Custom(logMessage)
        }
    }
}
