package com.a32b.plant

import android.app.Application
import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlantApplication : Application(){
    companion object{
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        installAppCheckProvider()
        appContext = applicationContext
        discardUnsentCrashReportsIfCollectionDisabled()
    }

    // Firestore·Functions 요청에 App Check 토큰을 붙인다 (#608)
    // 첫 요청 전에 설치해야 하므로 onCreate 맨 앞에서 한다. FirebaseApp은 이보다 먼저 FirebaseInitProvider가 초기화한다
    // 제공자는 빌드 타입별로 다르다 (src/release: Play Integrity, src/debug: 디버그 제공자)
    private fun installAppCheckProvider() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
    }

    // 수집이 꺼진 빌드(debug)에서도 SDK는 크래시를 기기에 보관만 해 둔다.
    // 같은 기기에 release를 설치하면 그 보고서가 함께 전송되므로, 켤 때마다 지운다 (#622)
    private fun discardUnsentCrashReportsIfCollectionDisabled() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (!crashlytics.isCrashlyticsCollectionEnabled) crashlytics.deleteUnsentReports()
    }
}