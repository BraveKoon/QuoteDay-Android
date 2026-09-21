import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // 버전을 여기 적는 이유는 루트 build.gradle.kts 의 주석을 보라.
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin.get()
    id("org.jetbrains.kotlin.plugin.serialization") version libs.versions.kotlin.get()
}

// 툴체인(jvmToolchain)을 쓰지 않는다. 이 저장소는 JDK 21 하나로 빌드하고,
// 바이트코드만 17 로 내린다. 안드로이드 모듈이 읽을 수 있어야 하기 때문이다.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
