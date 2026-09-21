package com.quoteday.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * iOS 와 **같은 값**이 나오는지 본다.
 *
 * 이 테스트가 이 모듈에서 가장 중요하다. 해시가 한 비트라도 다르면 같은 날
 * 아이폰과 안드로이드가 서로 다른 명언을 보여 주고, 위젯 딥링크가 엉뚱한 명언을
 * 가리키며, 하트가 다른 레코드에 쌓인다. 그런데 그 어느 것도 앱을 죽이지 않아서
 * 아무도 눈치채지 못한다.
 *
 * 기대값은 iOS 의 알고리즘을 그대로 재현해 뽑았다(tools/ 의 대조 스크립트 참고).
 */
class StableHashTest {

    @Test
    fun `fnv1a matches the iOS implementation`() {
        assertEquals(-3750763034362895579L, StableHash.fnv1a(""))
        assertEquals(-5808556873153909620L, StableHash.fnv1a("a"))
        assertEquals(3564432900538996650L, StableHash.fnv1a("quote:churchill-courage-to-continue"))
        assertEquals(1826493231146780032L, StableHash.fnv1a("daily:2026-09-21"))
        assertEquals(6413366418858192449L, StableHash.fnv1a("한글"))
    }

    @Test
    fun `index matches the iOS implementation`() {
        assertEquals(190, StableHash.index("daily:2026-09-21", 201))
        assertEquals(9, StableHash.index("cat:work:abc", 37))
        assertEquals(8, StableHash.index("", 10))
        assertEquals(143, StableHash.index("한글 seed", 1000))
    }

    /** 부호 없는 나머지를 쓰지 않으면 음수 인덱스가 나온다. 그러면 앱이 죽는다. */
    @Test
    fun `index never goes out of range`() {
        for (i in 0 until 2000) {
            val index = StableHash.index("seed:$i", 201)
            assertTrue(index in 0 until 201, "seed:$i 에서 $index 가 나왔다")
        }
    }

    @Test
    fun `stable uuid matches the iOS implementation`() {
        assertEquals(
            "2B7867B6-7CA3-44C9-B0E3-6D34F33343F0",
            StableHash.stableUuid("quote:churchill-courage-to-continue"),
        )
        assertEquals("556843DE-B707-47B8-BD4C-890053FA17B2", StableHash.stableUuid("quote:placeholder"))
        assertEquals("0166905A-4111-4900-A82C-BE09AC9F01D5", StableHash.stableUuid("한글"))
    }

    @Test
    fun `stable uuid is a valid version 4 uuid`() {
        val uuid = StableHash.stableUuid("quote:anything")
        assertTrue(Regex("^[0-9A-F]{8}-[0-9A-F]{4}-4[0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12}$")
            .matches(uuid), uuid)
    }
}
