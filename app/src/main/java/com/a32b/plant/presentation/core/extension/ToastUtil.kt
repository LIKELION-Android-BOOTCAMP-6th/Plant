package com.a32b.plant.presentation.core.extension

import android.content.Context
import android.widget.Toast

/**
 * String 메시지로 토스트 출력
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
