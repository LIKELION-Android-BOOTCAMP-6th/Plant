package com.a32b.plant.ui.feature.studying.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.repository.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

data class StudyResultUiState(
    val isDialogShow : Boolean = false
)
class StudyResultViewModel(
) : ViewModel(){

    private val _uiState = MutableStateFlow(StudyResultUiState())
    val uiState = _uiState.asStateFlow()

    fun onDialogShow() = _uiState.update { it.copy(isDialogShow = true) }
    fun onDialogDismiss() = _uiState.update { it.copy(isDialogShow = false) }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap){
        val fileName = "plant_${TimeFormatter.formatToDateOnly(LocalDateTime.now())}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }

}