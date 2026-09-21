package com.quoteday.core

import kotlinx.serialization.json.Json

/**
 * 번들에 들어 있는 명언·인물 데이터.
 *
 * 원본은 iOS 저장소의 Swift 파일이고, `tools/extract_from_swift.py` 가 JSON 으로
 * 옮긴다. 손으로 베끼지 않는 이유는 3,400 줄을 옮기면 반드시 어딘가 틀리는데
 * 그것이 명언 한 글자라면 아무도 눈치채지 못한 채 배포되기 때문이다.
 */
class QuoteLibrary private constructor(
    val quotes: List<Quote>,
    val authors: List<Author>,
    val behindStories: List<BehindStory>,
    private val disputedSlugs: Set<String>,
) {
    private val authorsById: Map<String, Author> = authors.associateBy { it.id }
    private val quotesBySlug: Map<String, Quote> = quotes.associateBy { it.slug }
    private val quotesById: Map<String, Quote> = quotes.associateBy { it.id }
    private val storiesBySlug: Map<String, BehindStory> = behindStories.associateBy { it.quoteSlug }
    private val quotesByCategory: Map<AppCategory, List<Quote>> =
        AppCategory.entries.associateWith { category -> quotes.filter { it.matches(category) } }

    val count: Int get() = quotes.size

    // Swift 는 `quote(slug:)` 와 `quote(id:)` 를 인자 레이블로 구분했지만
    // Kotlin/JVM 에는 레이블이 없어 같은 함수가 된다. 이름을 갈라 둔다.
    fun quoteBySlug(slug: String): Quote? = quotesBySlug[slug]
    fun quoteById(id: String): Quote? = quotesById[id]
    fun quotes(category: AppCategory): List<Quote> = quotesByCategory[category].orEmpty()
    fun quotes(authorId: String): List<Quote> = quotes.filter { it.authorId == authorId }
    fun author(id: String): Author? = authorsById[id]
    fun behindStory(slug: String): BehindStory? = storiesBySlug[slug]

    /**
     * 귀속이 논쟁 중인 명언인지.
     * 챌린지의 "누가 말했을까"에서 빼야 한다 — 그러지 않으면 앱이 확인되지 않은
     * 귀속을 정답이라고 가르치게 된다.
     */
    fun isDisputed(slug: String): Boolean = slug in disputedSlugs

    fun author(quote: Quote): Author = authorsById[quote.authorId] ?: unknownAuthor
    fun presentation(quote: Quote): QuotePresentation = QuotePresentation(quote, author(quote))
    fun presentationById(id: String): QuotePresentation? = quoteById(id)?.let { presentation(it) }

    fun search(term: String): List<Quote> {
        val needle = term.trim()
        if (needle.isEmpty()) return quotes
        val lowered = needle.lowercase()
        return quotes.filter { quote ->
            quote.text.contains(needle) ||
                quote.originalText?.lowercase()?.contains(lowered) == true ||
                author(quote).name.lowercase().contains(lowered) ||
                author(quote).koreanName?.contains(needle) == true
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** 데이터가 비어 있을 때 화면이 비지 않게 하는 자리 표시. */
        val placeholder = Quote(
            slug = "placeholder",
            text = "오늘 하루도 한 걸음.",
            authorId = "unknown",
            category = AppCategory.DAILY,
        )

        val unknownAuthor = Author(
            id = "unknown",
            name = "Unknown",
            koreanName = "미상",
            occupation = "미상",
            nationality = "미상",
            biography = "알려지지 않은 인물입니다.",
        )

        private fun <T> load(name: String, deserialize: (String) -> T): T {
            val stream = QuoteLibrary::class.java.getResourceAsStream("/data/$name")
                ?: error("데이터 파일을 찾을 수 없습니다: $name")
            return stream.bufferedReader(Charsets.UTF_8).use { deserialize(it.readText()) }
        }

        val shared: QuoteLibrary by lazy {
            QuoteLibrary(
                quotes = load("quotes.json") { json.decodeFromString(it) },
                authors = load("authors.json") { json.decodeFromString(it) },
                behindStories = load("behind_stories.json") { json.decodeFromString(it) },
                disputedSlugs = load("disputed_slugs.json") {
                    json.decodeFromString<List<String>>(it).toSet()
                },
            )
        }
    }
}
