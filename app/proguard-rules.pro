# kotlinx.serialization 은 리플렉션 대신 생성된 serializer 를 쓰지만,
# 그 serializer 를 찾는 경로가 클래스 이름에 기대고 있다.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.quoteday.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.quoteday.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
