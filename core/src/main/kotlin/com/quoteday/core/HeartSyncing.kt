package com.quoteday.core

/**
 * 하트를 다른 사용자와 주고받는 통로.
 *
 * 인터페이스로 끊어 두는 이유는 iOS 와 같다.
 * - 서버를 바꾸어도 화면과 저장소는 그대로 둘 수 있다.
 * - 테스트는 가짜 구현으로 돌린다.
 *
 * iOS 는 이 자리에 CloudKit 공개 데이터베이스를 끼운다. **안드로이드는 CloudKit 에
 * 접근할 수 없다** — 애플 전용이다. 그래서 지금은 [OfflineHeartSync] 만 있고,
 * 하트 수는 기기 안에서만 쌓인다. 두 플랫폼을 합치려면 공용 백엔드를 만들고
 * 양쪽에서 이 인터페이스 뒤에 끼우면 된다.
 */
interface HeartSyncing {
    /** 지금 주고받을 수 있는 상태인지. */
    suspend fun availability(): HeartSyncAvailability

    /** 여러 명언의 **전체 하트 수**를 한 번에 읽는다. 없는 명언은 결과에서 빠진다. */
    suspend fun counts(slugs: List<String>): Map<String, Int>

    /** 내가 하트를 누른 명언들. */
    suspend fun myHearts(among: List<String>): Set<String>

    /**
     * 하트를 켜거나 끈다.
     * @return 반영된 뒤의 전체 하트 수.
     */
    suspend fun setHeart(isOn: Boolean, slug: String): Int
}

enum class HeartSyncAvailability {
    /** 주고받을 수 있다. */
    AVAILABLE,

    /** 서버가 설정되지 않았다. 하트는 기기 안에만 쌓인다. */
    NOT_CONFIGURED,

    /** 설정은 되어 있으나 지금 닿지 못한다. */
    UNAVAILABLE,
}

/**
 * 동기화가 없는 구현.
 *
 * **하트를 막지 않는다.** 누르는 것은 되고, 합계만 내 것만 센다. 눌러도 아무
 * 반응이 없는 하트보다는 이쪽이 낫다.
 */
class OfflineHeartSync : HeartSyncing {
    override suspend fun availability(): HeartSyncAvailability = HeartSyncAvailability.NOT_CONFIGURED
    override suspend fun counts(slugs: List<String>): Map<String, Int> = emptyMap()
    override suspend fun myHearts(among: List<String>): Set<String> = emptySet()
    override suspend fun setHeart(isOn: Boolean, slug: String): Int = if (isOn) 1 else 0
}
