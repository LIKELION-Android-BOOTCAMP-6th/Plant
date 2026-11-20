// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    //firebase sdk
    id("com.google.gms.google-services") version "4.4.4" apply false
    // 크래시 수집 (#622)
    id("com.google.firebase.crashlytics") version "3.0.8" apply false
}