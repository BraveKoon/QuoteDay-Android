package com.quoteday.core

import kotlin.random.Random

/**
 * seed 문자열 하나로 완전히 재현되는 난수원.
 *
 * 화면이 다시 그려질 때마다 보기 순서가 뒤바뀌면 안 되므로 결정적이어야 한다.
 *
 * ## iOS 와 같은 값이 나오지 않는다 — 그래도 괜찮다
 *
 * Swift 의 `Array.shuffled(using:)` 이 내부적으로 난수를 몇 번 어떻게 꺼내는지는
 * 표준 라이브러리 구현에 달려 있어 Kotlin 에서 그대로 재현할 수 없다.
 *
 * 그런데 챌린지는 **판마다 새 seed(UUID)** 를 쓴다. 같은 기기에서 두 번 해도
 * 다른 문제가 나오는 것이 정상이므로, 두 플랫폼이 같은 문제를 낼 이유가 없다.
 *
 * 반대로 플랫폼 간에 **반드시 같아야 하는 것**은 따로 있고, 그쪽은
 * [StableHash] 가 비트 단위로 맞춰 둔다 — 오늘의 명언, 딥링크 식별자, 점수 구간.
 */
class SeededRandom(seed: String) : Random() {
    private var state: Long = StableHash.fnv1a(seed)

    /**
     * 0 을 먼저 걸러야 한다. `Long` 의 `ushr` 은 자리수를 63 으로 나눈 나머지만큼만
     * 밀기 때문에 `ushr 64` 가 `ushr 0` 이 되어 버린다. 그러면 비트를 하나도
     * 요구하지 않았는데 64비트가 통째로 나온다.
     *
     * 이 경로는 실제로 밟힌다. 원소가 하나뿐인 목록에 `random()` 을 부르면
     * 코틀린이 `nextBits(0)` 을 부르고, 0 대신 음수가 돌아오면 그 자리에서
     * 인덱스 예외가 난다.
     */
    override fun nextBits(bitCount: Int): Int =
        if (bitCount == 0) 0 else (nextLong() ushr (64 - bitCount)).toInt()

    override fun nextLong(): Long {
        state += -0x61c8864680b583ebL          // 0x9E3779B97F4A7C15
        return StableHash.mix(state)
    }
}

/** 이 목록을 seed 에 따라 항상 같은 순서로 섞는다. */
fun <T> List<T>.shuffledWith(random: SeededRandom): List<T> = shuffled(random)
