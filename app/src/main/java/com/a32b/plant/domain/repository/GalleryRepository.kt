package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result
import java.io.OutputStream

interface GalleryRepository {

    /** 현재 기기에서 갤러리 저장이 가능한지 여부 */
    fun isGallerySaveSupported(): Boolean

    /** 이미지를 갤러리(Pictures)에 저장 (write: 저장 스트림에 이미지를 쓰고 성공 여부 반환) */
    suspend fun saveImageToGallery(fileName: String, mimeType: String, write: (OutputStream) -> Boolean): Result<Unit>
}
