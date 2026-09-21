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
