package com.a32b.plant.data.repository

import android.net.Uri
import android.os.Build
import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.local.GalleryLocalDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.GalleryRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val SAVE_FAILED_MESSAGE = "이미지 저장에 실패했습니다."

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    private val local: GalleryLocalDataSource
) : GalleryRepository {

    // 저장 경로 지정(RELATIVE_PATH)과 숨김 저장(IS_PENDING)이 Android 10(API 29)부터 지원되고,
    // 그 이하는 저장소 권한까지 필요하므로 저장 기능 자체를 지원하지 않음
    override fun isGallerySaveSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override suspend fun savePngToGallery(pngBytes: ByteArray, fileName: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isGallerySaveSupported()) {
                Log.e("Gallery", "지원하지 않는 버전에서 이미지 저장 시도 (SDK ${Build.VERSION.SDK_INT})")
                return@withContext Result.Failure(AppError.Local(SAVE_FAILED_MESSAGE))
            }

            var uri: Uri? = null
            safeRunCatching {
                if (pngBytes.isEmpty()) error("PNG 변환 실패")
                val insertedUri = local.insertPendingImage(fileName, "image/png") ?: error("갤러리 항목 생성 실패")
                uri = insertedUri
                local.writeImage(insertedUri, pngBytes)
                if (local.publishImage(insertedUri) <= 0) error("숨김 해제 실패")
            }.fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { e ->
                    Log.e("Gallery", "이미지 저장 실패: ${e.message}", e)
                    // 실패하면 갤러리에 빈 이미지 항목이 남지 않도록 삭제
                    uri?.let { safeRunCatching { local.deleteImage(it) } }
                    Result.Failure(AppError.Local(SAVE_FAILED_MESSAGE))
                }
            )
        }
}
