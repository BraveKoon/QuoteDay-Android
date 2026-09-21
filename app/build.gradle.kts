import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 는 루트에서 선언하지 않는다 — 이유는 루트 build.gradle.kts 의 주석을 보라.
    // 여기서는 버전과 함께 요청한다. 이 플러그인은 빌드 클래스패스에 없기 때문이다.
    id("com.android.application") version libs.versions.agp.get()
    // 반대로 코틀린 플러그인은 루트의 `kotlin-jvm`·`serialization` 선언을 통해 이미
    // 클래스패스에 올라와 있다. 여기서 버전까지 적으면 Gradle 이
    // "already on the classpath with an unknown version" 이라며 거절한다.
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // Compose 컴파일러는 별도 아티팩트라 클래스패스에 없다. 버전이 필요하고,
    // 코틀린과 같은 버전이어야 한다.
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
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
