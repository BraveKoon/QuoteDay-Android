// 루트에는 플러그인을 하나도 선언하지 않는다.
//
// 이유가 둘이다.
//
// 1. 루트에서 `apply false` 로 선언만 해도 Gradle 이 플러그인 마커를 받으러 나간다.
//    Android SDK 도 구글 저장소도 없는 개발 컨테이너에서는 그 순간 :core:test 까지
//    같이 죽는다.
// 2. 루트에서 코틀린 플러그인을 올리면 그것이 **부모 클래스로더**에 들어간다.
//    그러면 :app 이 자기 스크립트에서 AGP 를 올릴 때, 부모에 있는 코틀린 플러그인이
//    자식에 있는 AGP 클래스를 보지 못해 이렇게 죽는다:
//        Could not generate a decorated class for type KotlinAndroidTarget
//          > com/android/build/gradle/api/BaseVariant
//
// 그래서 모듈마다 필요한 플러그인을 버전과 함께 직접 선언한다. 한 스크립트 안에서
// 함께 올라가므로 서로를 본다. 버전은 gradle/libs.versions.toml 한 곳에 있다.
