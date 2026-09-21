package com.quoteday.core

import java.time.LocalDate

/**
 * 명언 선택 규칙.
 *
 * 모든 선택이 **결정적**이다. 같은 (날짜, 카테고리, seed) 면 앱을 다시 켜도,
 * 위젯 프로세스에서 계산해도 같은 결과가 나온다. 무작위를 쓰지 않는 이유는
 * 위젯과 앱 본체가 서로 다른 프로세스에서 같은 "오늘의 명언"을 보여야 하기 때문이다.
 */
class QuoteService(private val library: QuoteLibrary = QuoteLibrary.shared) {

    // ------------------------------------------------------------ 오늘의 명언

    /** 날짜 → 해시 → 인덱스. 자정이 지나면 자동으로 다음 명언으로 넘어간다. */
    fun quoteOfTheDay(date: LocalDate = LocalDate.now()): Quote {
        val pool = library.quotes
        if (pool.isEmpty()) return QuoteLibrary.placeholder
        return pool[StableHash.index("daily:${dayKey(date)}", pool.size)]
    }

    /** 설정에서 선호 카테고리를 골랐을 때의 오늘의 명언. */
    fun quoteOfTheDay(date: LocalDate, preferred: AppCategory?): Quote {
        if (preferred == null) return quoteOfTheDay(date)
        val pool = library.quotes(preferred)
        if (pool.isEmpty()) return quoteOfTheDay(date)
        return pool[StableHash.index("daily:${preferred.rawValue}:${dayKey(date)}", pool.size)]
    }

    fun presentationOfTheDay(
        date: LocalDate = LocalDate.now(),
        preferred: AppCategory? = null,
    ): QuotePresentation = library.presentation(quoteOfTheDay(date, preferred))

    // ------------------------------------------------------------ 카테고리

    /**
     * 카테고리에 어울리는 명언.
     *
     * @param seed 같은 일정에 늘 같은 명언이 나오게 하는 키.
     *   일정 ID + 시작 시각을 넣으면 일정을 고치기 전까지 명언이 고정된다.
     */
    fun quote(category: AppCategory, seed: String): Quote {
        val pool = candidatePool(category)
        if (pool.isEmpty()) return QuoteLibrary.placeholder
        return pool[StableHash.index("cat:${category.rawValue}:$seed", pool.size)]
    }

    /** 폴백까지 적용한 후보 목록. */
    fun candidatePool(category: AppCategory): List<Quote> {
        val seen = LinkedHashSet<String>()
        val pool = mutableListOf<Quote>()

        fun append(quotes: List<Quote>) {
            for (quote in quotes) if (seen.add(quote.slug)) pool += quote
        }

        append(library.quotes(category))
        // 후보가 너무 적으면 매번 같은 명언만 보이므로 이웃 카테고리로 넓힌다.
        if (pool.size < MINIMUM_POOL_SIZE) {
            for (related in category.related) {
                append(library.quotes(related))
                if (pool.size >= MINIMUM_POOL_SIZE) break
            }
        }
        if (pool.isEmpty()) append(library.quotes)
        return pool
    }

    fun quote(scheduleId: String, startEpochSeconds: Long, category: AppCategory): Quote =
        quote(category, "$scheduleId:$startEpochSeconds")

    // ------------------------------------------------------------ 조회

    fun quoteBySlug(slug: String): Quote? = library.quoteBySlug(slug)
    fun quoteById(id: String): Quote? = library.quoteById(id)
    fun author(quote: Quote): Author = library.author(quote)
    fun presentation(quote: Quote): QuotePresentation = library.presentation(quote)
    fun quotes(category: AppCategory): List<Quote> = library.quotes(category)
    fun search(term: String): List<Quote> = library.search(term)
    val allQuotes: List<Quote> get() = library.quotes
    val quoteCount: Int get() = library.count

    companion object {
        /** 후보가 이보다 적으면 이웃 카테고리를 끌어온다. */
        const val MINIMUM_POOL_SIZE = 6

        /** 로컬 자정 기준 "yyyy-MM-dd". 오늘의 명언 seed 다. */
        fun dayKey(date: LocalDate): String =
            "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }
}
