plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.a32b.plant"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.a32b.plant"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //파이어베이스 버전 관리자 bom
    implementation (platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")

    //파이어베이스 인증(이메일/구글)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    //구글 로그인 최신 버전 후순위 개발로 미뤄둘 것 -> '주석 해제'
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    ///파이어스토어
    implementation("com.google.firebase:firebase-firestore")
    //realtime-database
    implementation("com.google.firebase:firebase-database")
    //fcm
    implementation("com.google.firebase:firebase-messaging")

    implementation(libs.compose.nav)
    implementation(libs.coroutine.core)
    implementation(libs.coroutine.android)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.serialization)

    implementation(libs.splashscreen)

    implementation("androidx.compose.material:material-icons-extended")

    //coil, AsyniImage
    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation(libs.datastore.preference)
}