package com.a32b.plant.data.source.local

import android.net.Uri
import java.io.OutputStream

interface GalleryLocalDataSource {
    /** 갤러리에 숨김 상태의 빈 이미지 항목 생성 (실패 시 null) */
    fun insertPendingImage(fileName: String, mimeType: String): Uri?

    /** 생성된 항목에 이미지 데이터를 쓸 출력 스트림 열기 (실패 시 null) */
    fun openOutputStream(uri: Uri): OutputStream?

    /** 숨김 해제 후 갤러리에 노출 (갱신된 항목 수 반환) */
    fun publishImage(uri: Uri): Int

    /** 항목 삭제 */
    fun deleteImage(uri: Uri)
}
