package com.a32b.plant.presentation.core.extension

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * 웹 주소를 브라우저 앱으로 연다. 열 수 있는 앱이 없으면 토스트로 안내한다
 * (마이페이지·첫 로그인 닉네임 설정 다이얼로그의 약관·개인정보처리방침 열기)
 */
fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (error: Exception) {
        showToast("브라우저 앱을 찾을 수 없습니다.")
    }
}
