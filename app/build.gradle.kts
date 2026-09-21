import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 네 개를 모두 이 스크립트에서 버전과 함께 올린다. 한 클래스로더에 함께 있어야
    // 코틀린 플러그인이 AGP 의 클래스를 볼 수 있다 — 루트 build.gradle.kts 의 주석을 보라.
    id("com.android.application") version libs.versions.agp.get()
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

android {
    namespace = "com.quoteday.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quoteday.app"
        // 26 아래로 내리지 않는다. 알림 채널과 java.time 이 26부터다.
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("ko")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // :core 와 같은 이유로 툴체인을 쓰지 않는다 — JDK 21 로 빌드하고 바이트코드만 17 이다.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].java.srcDirs("src/main/kotlin")

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

// `kotlin { }` 은 android 블록 밖이다. 안에 넣으면 AGP 의 확장이 아니라서 해석되지 않는다.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
}
