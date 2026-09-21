package com.quoteday.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuoteServiceTest {

    private val service = QuoteService()

    /**
     * 같은 날짜면 아이폰과 안드로이드가 **같은 명언**을 보여 주어야 한다.
     * 기대 slug 는 iOS 의 계산을 재현해 뽑았다.
     */
    @Test
    fun `quote of the day matches iOS for the same date`() {
        val expected = mapOf(
            LocalDate.of(2026, 1, 1) to "twain-laughter-weapon",
            LocalDate.of(2026, 6, 15) to "feynman-questions-unanswered",
            LocalDate.of(2026, 9, 21) to "voltaire-perfect-enemy-of-good",
            LocalDate.of(2027, 2, 28) to "drucker-efficiency-effectiveness",
            LocalDate.of(1999, 12, 31) to "gibran-spaces-in-your-togetherness",
        )
        for ((date, slug) in expected) {
            assertEquals(slug, service.quoteOfTheDay(date).slug, "$date")
        }
    }

    @Test
    fun `quote of the day is stable within a day and changes across days`() {
        val today = LocalDate.of(2026, 5, 4)
        assertEquals(service.quoteOfTheDay(today).slug, service.quoteOfTheDay(today).slug)

        val slugs = (0 until 30).map { service.quoteOfTheDay(today.plusDays(it.toLong())).slug }
        assertTrue(slugs.toSet().size > 20, "한 달에 20편도 안 바뀌면 같은 명언만 보인다")
    }

    @Test
    fun `preferred category restricts the daily quote`() {
        val date = LocalDate.of(2026, 3, 9)
        for (category in AppCategory.selectableForQuotes) {
            val quote = service.quoteOfTheDay(date, category)
            assertTrue(quote.matches(category), "${category.rawValue}: ${quote.slug}")
        }
    }

    @Test
    fun `candidate pool falls back to related categories`() {
        for (category in AppCategory.entries) {
            val pool = service.candidatePool(category)
            assertTrue(pool.isNotEmpty(), "${category.rawValue} 후보가 비었다")
            assertTrue(
                pool.size >= QuoteService.MINIMUM_POOL_SIZE,
                "${category.rawValue} 후보가 ${pool.size}개뿐이다",
            )
            assertEquals(pool.size, pool.map { it.slug }.toSet().size, "후보에 중복이 있다")
        }
    }

    @Test
    fun `the same schedule always gets the same quote`() {
        val id = "3F2504E0-4F89-41D3-9A0C-0305E82C3301"
        val first = service.quote(id, 1_760_000_000L, AppCategory.STUDY)
        val second = service.quote(id, 1_760_000_000L, AppCategory.STUDY)
        assertEquals(first.slug, second.slug)

        val moved = service.quote(id, 1_760_003_600L, AppCategory.STUDY)
        assertTrue(moved.slug != first.slug || service.candidatePool(AppCategory.STUDY).size == 1)
    }

    @Test
    fun `library is complete and self consistent`() {
        assertEquals(201, service.quoteCount)
        val slugs = service.allQuotes.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size, "slug 가 중복된다")
        for (quote in service.allQuotes) {
            assertTrue(quote.text.isNotBlank(), "${quote.slug} 본문이 비었다")
            assertNotNull(service.author(quote), "${quote.slug} 인물이 없다")
            assertTrue(service.author(quote).id != "unknown", "${quote.slug} 인물을 못 찾았다")
        }
    }

    @Test
    fun `search finds by text author name and korean name`() {
        assertTrue(service.search("용기").isNotEmpty())
        assertTrue(service.search("Churchill").isNotEmpty())
        assertTrue(service.search("처칠").isNotEmpty())
        assertEquals(service.quoteCount, service.search("   ").size, "빈 검색어는 전체를 준다")
    }
}
