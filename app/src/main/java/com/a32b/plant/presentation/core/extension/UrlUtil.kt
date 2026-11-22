package com.a32b.plant.presentation.core.extension

import android.content.ActivityNotFoundException
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

/**
 * Play 스토어 앱에서 이 앱의 상세 화면을 연다 (업데이트 안내, #662).
 * Play 스토어 앱이 없으면 웹 Play 스토어 주소를 브라우저로 연다
 */
fun Context.openPlayStoreDetails() {
    // 다른 스토어 앱도 market:// 주소를 받을 수 있어 Play 스토어 앱으로 지정한다
    val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        .setPackage(PLAY_STORE_PACKAGE)
    try {
        startActivity(storeIntent)
    } catch (error: ActivityNotFoundException) {
        openUrl("https://play.google.com/store/apps/details?id=$packageName")
    }
}

private const val PLAY_STORE_PACKAGE = "com.android.vending"
