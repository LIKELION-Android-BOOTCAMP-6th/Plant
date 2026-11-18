import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.aboutlibraries.android)
}

// 릴리스 서명 값: local.properties를 먼저 보고, 없으면 같은 이름의 환경변수를 본다 (#621)
// 값이 하나도 없으면(CI·팀원 PC) 서명을 생략해 디버그 빌드에는 영향이 없다
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String): String? =
    (localProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() }

val releaseSigningKeys = listOf(
    "RELEASE_STORE_FILE",
    "RELEASE_STORE_PASSWORD",
    "RELEASE_KEY_ALIAS",
    "RELEASE_KEY_PASSWORD"
)
val releaseSigningValues = releaseSigningKeys.associateWith { signingValue(it) }
// 상대 경로는 프로젝트 루트 기준
val releaseStoreFile = releaseSigningValues["RELEASE_STORE_FILE"]?.let { rootProject.file(it) }
val hasReleaseSigning = releaseSigningValues.values.all { it != null } && releaseStoreFile?.exists() == true

if (!hasReleaseSigning && releaseSigningValues.values.any { it != null }) {
    // 값은 출력하지 않고 빠진 키 이름만 알린다
    val missing = releaseSigningValues.filterValues { it == null }.keys.toMutableList()
    if (releaseStoreFile != null && !releaseStoreFile.exists()) missing += "RELEASE_STORE_FILE(파일 없음)"
    logger.warn("릴리스 서명 설정이 불완전해 release 서명을 생략합니다. 확인할 항목: ${missing.joinToString()}")
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
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseSigningValues["RELEASE_STORE_PASSWORD"]
                keyAlias = releaseSigningValues["RELEASE_KEY_ALIAS"]
                keyPassword = releaseSigningValues["RELEASE_KEY_PASSWORD"]
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
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

// 오픈소스 라이선스 목록: 빌드할 때 의존성에서 res/raw/aboutlibraries.json을 자동 생성 (#637)
aboutLibraries {
    collect {
        // 의존성이 아닌 항목(폰트)과 그 라이선스는 이 폴더의 libraries/, licenses/ JSON으로 직접 추가
        configPath = file("aboutlibraries")
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
    testImplementation(libs.coroutine.test)
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
    //function
    implementation("com.google.firebase:firebase-functions")

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

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.konfetti.compose)
    implementation(libs.aboutlibraries.compose.m3)
}
