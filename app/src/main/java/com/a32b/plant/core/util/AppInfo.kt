package com.a32b.plant.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 설치된 앱의 versionName을 읽는다. 값이 없으면 "unknown"을 반환한다.
 */
fun Context.appVersionName(): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0)
    }
    return packageInfo.versionName ?: UNKNOWN_APP_VERSION
}

private const val UNKNOWN_APP_VERSION = "unknown"
