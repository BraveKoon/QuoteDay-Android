package com.quoteday.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SeededRandomTest {

    @Test
    fun `the same seed replays the same numbers`() {
        val first = List(20) { SeededRandom("replay").nextInt(1_000) }
        val second = List(20) { SeededRandom("replay").nextInt(1_000) }
        assertEquals(first, second)
    }

    @Test
    fun `different seeds diverge`() {
        val first = SeededRandom("a").let { rng -> List(20) { rng.nextInt(1_000) } }
        val second = SeededRandom("b").let { rng -> List(20) { rng.nextInt(1_000) } }
        assertNotEquals(first, second)
    }

    /**
     * 원소가 하나뿐인 목록에 `random()` 을 부르면 코틀린이 `nextBits(0)` 을 부른다.
     * 0 을 돌려주지 않으면 그 자리에서 인덱스 예외가 난다 — 챌린지의 인물 후보
     * 풀이 한 명으로 좁혀질 때 실제로 밟히는 길이다.
     */
    @Test
    fun `picking from a single element list works`() {
        val rng = SeededRandom("single")
        repeat(100) { assertEquals("only", listOf("only").random(rng)) }
        repeat(100) { assertEquals(0, rng.nextBits(0)) }
    }

    @Test
    fun `bounded values stay inside the range`() {
        val rng = SeededRandom("bounds")
        repeat(10_000) {
            val value = rng.nextInt(7)
            assertTrue(value in 0..6, "범위를 벗어났습니다: $value")
        }
    }

    /** 한쪽으로 쏠리면 문제가 늘 같은 낱말로 채워진다. */
    @Test
    fun `values spread across the range`() {
        val rng = SeededRandom("spread")
        val counts = IntArray(10)
        repeat(10_000) { counts[rng.nextInt(10)] += 1 }
        for ((bucket, count) in counts.withIndex()) {
            assertTrue(count in 800..1_200, "$bucket 칸이 $count 번 나왔습니다.")
        }
    }
}
