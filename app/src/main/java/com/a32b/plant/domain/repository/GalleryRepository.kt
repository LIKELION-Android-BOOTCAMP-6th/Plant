package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result

interface GalleryRepository {

    /** 현재 기기에서 갤러리 저장이 가능한지 여부 */
    fun isGallerySaveSupported(): Boolean

    /** PNG 이미지를 갤러리(Pictures)에 저장 */
    suspend fun savePngToGallery(pngBytes: ByteArray, fileName: String): Result<Unit>
}
