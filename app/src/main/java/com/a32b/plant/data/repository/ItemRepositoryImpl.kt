package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.error.toFunctionsAppError
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.source.remote.item.ItemRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRepositoryImpl @Inject constructor(
    private val itemRemoteDataSource: ItemRemoteDataSource
): ItemRepository {

    override suspend fun openBonusBoxes(boxCount: Int, requestId: String): Result<List<ItemType>> = safeRunCatching {
        itemRemoteDataSource.openBonusBoxes(boxCount, requestId)
    }.fold(
        onSuccess = { Result.Success(it.toDomain()) },
        onFailure = { e ->
            Log.e("ItemRepository", "보너스 박스 열기 함수 호출 실패: ${e.message}", e)
            // 박스 부족 등 서버가 보낸 안내 문구는 AppError.Custom으로, 오프라인은 AppError.Network로 바뀐다
            Result.Failure(e.toFunctionsAppError())
        }
    )

    override fun getBoxCount(uid: String): Flow<Int> =
        itemRemoteDataSource.observeBoxCount(uid)

    private fun handleError(e: Throwable, logMessage: String): AppError {
        Log.e("ItemRepository", "$logMessage: ${e.message}", e)

        return when (e) {
            is FirebaseNetworkException -> AppError.Network()
            is IllegalStateException -> AppError.Custom(e.message ?: logMessage) //FirebaseFirestoreException 내부로 들어가진 않는지 확인 필요
            else -> AppError.Custom(logMessage)
        }
    }
}
