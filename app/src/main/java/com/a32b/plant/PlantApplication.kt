package com.a32b.plant

import android.app.Application
import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PlantApplication : Application(){
    companion object{
        lateinit var appContext: Context
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        discardUnsentCrashReportsIfCollectionDisabled()
    }

    // 수집이 꺼진 빌드(debug)에서도 SDK는 크래시를 기기에 보관만 해 둔다.
    // 같은 기기에 release를 설치하면 그 보고서가 함께 전송되므로, 켤 때마다 지운다 (#622)
    private fun discardUnsentCrashReportsIfCollectionDisabled() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (!crashlytics.isCrashlyticsCollectionEnabled) crashlytics.deleteUnsentReports()
    }
}