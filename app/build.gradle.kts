import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    // 네 개를 모두 이 스크립트에서 버전과 함께 올린다. 한 클래스로더에 함께 있어야
    // 코틀린 플러그인이 AGP 의 클래스를 볼 수 있다 — 루트 build.gradle.kts 의 주석을 보라.
    id("com.android.application") version libs.versions.agp.get()
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

/**
 * 서명 정보.
 *
 * 저장소에는 **넣지 않는다**. 키스토어가 새면 다른 사람이 이 앱의 업데이트를
 * 낼 수 있게 된다. 로컬에서는 `keystore.properties`(gitignore 되어 있다),
 * CI 에서는 환경 변수로 받는다. 어느 쪽도 없으면 서명 없이 빌드한다 —
 * 그래야 서명 정보가 없는 곳에서도 릴리스 빌드가 컴파일되는지 확인할 수 있다.
 */
val keystoreProperties = Properties().also { props ->
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(props::load)
}

fun releaseSecret(key: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(environmentVariable)

val releaseStorePath: String? = releaseSecret("storeFile", "QUOTEDAY_STORE_FILE")

android {
    namespace = "com.quoteday.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quoteday.app"
        // 26 아래로 내리지 않는다. 알림 채널과 java.time 이 26부터다.
        minSdk = 26
        targetSdk = 35
        // 릴리스 워크플로가 `-PversionCode=...` 로 덮어쓸 수 있다.
        // 스토어는 같은 versionCode 를 두 번 받지 않으므로, 올릴 때마다 올려야 한다.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("ko")
    }

    signingConfigs {
        create("release") {
            if (releaseStorePath != null) {
                storeFile = file(releaseStorePath)
                storePassword = releaseSecret("storePassword", "QUOTEDAY_STORE_PASSWORD")
                keyAlias = releaseSecret("keyAlias", "QUOTEDAY_KEY_ALIAS")
                keyPassword = releaseSecret("keyPassword", "QUOTEDAY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // 서명 정보가 없으면 서명하지 않는다. 그대로 두면 디버그 키로 서명되어,
            // 스토어에 올릴 수 없는 파일이 "릴리스"라는 이름으로 나온다.
            signingConfig = if (releaseStorePath != null) signingConfigs.getByName("release") else null
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
    implementation(libs.androidx.glance.appwidget)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // 에뮬레이터에서 도는 테스트. 이 저장소에서 앱을 **실제로 켜 보는** 유일한 길이다.
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
