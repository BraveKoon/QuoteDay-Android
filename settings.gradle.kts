pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuoteDay"

// :core 는 안드로이드에 의존하지 않는 순수 Kotlin 이다.
// 명언 데이터와 알고리즘이 여기 있고, 그래서 이 컨테이너에서도 컴파일하고 테스트할 수 있다.
include(":core")

// :app 은 Android SDK 와 AGP 가 있어야 **설정**조차 된다. 플러그인을 선언만 해도
// Gradle 이 그것을 받으러 나가기 때문에, SDK 가 없는 곳에서는 :core:test 까지 같이 죽는다.
//
// 그래서 SDK 가 보일 때만 넣는다. 개발 컨테이너(SDK 없음)에서는 :core 를 테스트할 수 있고,
// CI 에서는 `android-actions/setup-android` 가 ANDROID_HOME 을 채우므로 :app 이 들어온다.
// CI 가 `:app:assembleDebug` 를 부르므로, 혹시 빠지면 "project not found" 로 시끄럽게 실패한다.
val androidSdk = System.getenv("ANDROID_HOME")
    ?: System.getenv("ANDROID_SDK_ROOT")
    ?: file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.startsWith("sdk.dir=") }
        ?.removePrefix("sdk.dir=")

if (androidSdk != null) {
    include(":app")
} else {
    logger.lifecycle("Android SDK 를 찾지 못해 :app 을 건너뜁니다. :core 만 빌드합니다.")
}
