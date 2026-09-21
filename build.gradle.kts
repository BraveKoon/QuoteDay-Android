plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    // 안드로이드용 플러그인(AGP, kotlin-android, compose)은 여기서 선언하지 않는다.
    // `apply false` 라도 Gradle 이 플러그인 마커를 받으러 나가는데, Android SDK 도
    // 구글 저장소도 없는 환경에서는 그 순간 :core:test 까지 같이 죽는다.
    // :app 은 자기 빌드 파일에서 버전과 함께 직접 선언한다.
}
