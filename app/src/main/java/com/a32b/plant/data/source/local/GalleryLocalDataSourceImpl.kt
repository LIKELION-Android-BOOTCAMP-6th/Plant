package com.a32b.plant.data.source.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryLocalDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GalleryLocalDataSource {

    private val resolver get() = context.contentResolver

    override fun insertPendingImage(fileName: String, mimeType: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            // 쓰는 도중에는 갤러리에 노출되지 않도록 숨김 (완료되지 못한 항목은 시스템이 정리)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override fun writeImage(uri: Uri, bytes: ByteArray) {
        val stream = resolver.openOutputStream(uri) ?: error("출력 스트림 열기 실패")
        stream.use { it.write(bytes) }
    }

    override fun publishImage(uri: Uri): Int {
        val pendingCleared = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
        return resolver.update(uri, pendingCleared, null, null)
    }

    override fun deleteImage(uri: Uri) {
        resolver.delete(uri, null, null)
    }
}
