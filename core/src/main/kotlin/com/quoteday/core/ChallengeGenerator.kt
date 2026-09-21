package com.quoteday.core

/**
 * 명언 데이터에서 챌린지 문제를 만든다.
 *
 * 순수 함수다 — 저장소도 화면도 건드리지 않고, 같은 seed 에는 같은 문제를 돌려준다.
 * (같은 플랫폼 안에서만이다. 판마다 새 seed 를 쓰므로 iOS 와 같은 문제가 나올
 * 필요는 없다 — [SeededRandom] 의 설명을 보라.)
 */
class ChallengeGenerator(private val library: QuoteLibrary = QuoteLibrary.shared) {

    /**
     * slug → 빈칸 후보 낱말.
     *
     * 문제 하나를 만들 때마다 200편 넘는 문장을 다시 자르지 않으려고 생성기를
     * 만들 때 한 번만 계산한다.
     */
    private val wordsBySlug: Map<String, List<String>> =
        library.quotes.associate { it.slug to BlankMaker.candidates(it.text) }

    // MARK: 한 판 만들기

    /**
     * 서로 다른 명언에서 문제를 뽑아 한 판을 만든다.
     *
     * 같은 명언이 한 판에 두 번 나오지 않는다. 후보가 모자라면 나온 만큼만 돌려준다.
     */
    fun makeRound(
        mode: ChallengeMode,
        difficulty: ChallengeDifficulty,
        seed: String,
        count: Int = QUESTIONS_PER_ROUND,
    ): List<ChallengeQuestion> {
        val random = SeededRandom("round:${mode.rawValue}:${difficulty.level}:$seed")
        val pool = sourceQuotes(mode).shuffledWith(random)

        val questions = mutableListOf<ChallengeQuestion>()
        for (quote in pool) {
            if (questions.size >= count) break
            makeQuestion(quote, mode, difficulty, "$seed:${quote.slug}")?.let { questions += it }
        }
        return questions
    }

    /**
     * 문제로 쓸 수 있는 명언.
     *
     * "누가 말했을까"에서는 귀속이 확인되지 않은 명언을 뺀다. 정답을 하나로 못 박는
     * 형식이라, 그대로 내면 앱이 확인되지 않은 귀속을 정답이라고 가르치게 된다.
     */
    fun sourceQuotes(mode: ChallengeMode): List<Quote> = when (mode) {
        ChallengeMode.FILL_IN_THE_BLANK ->
            library.quotes.filter { !wordsBySlug[it.slug].isNullOrEmpty() }
        ChallengeMode.GUESS_THE_AUTHOR ->
            library.quotes.filter { !library.isDisputed(it.slug) }
    }

    // MARK: 문제 하나 만들기

    fun makeQuestion(
        quote: Quote,
        mode: ChallengeMode,
        difficulty: ChallengeDifficulty,
        seed: String,
    ): ChallengeQuestion? = when (mode) {
        ChallengeMode.FILL_IN_THE_BLANK -> makeBlankQuestion(quote, difficulty, seed)
        ChallengeMode.GUESS_THE_AUTHOR -> makeAuthorQuestion(quote, difficulty, seed)
    }

    // MARK: 빈칸 채우기

    private fun makeBlankQuestion(
        quote: Quote,
        difficulty: ChallengeDifficulty,
        seed: String,
    ): ChallengeQuestion? {
        val random = SeededRandom("blank:${difficulty.level}:$seed")
        val author = library.author(quote)

        val blanked = BlankMaker.make(
            text = quote.text,
            blankCount = difficulty.blankCount,
            preferLongWords = difficulty.similarity != ChallengeSimilarity.RANDOM,
            random = random,
        ) ?: return null

        val answer = blanked.answers.joinToString(PAIR_SEPARATOR)

        // 오답 후보를 그럴듯한 순서로 **나눠서** 쌓는다.
        //
        // 한 배열에 이어 붙이면 안 된다. 전체 낱말이 1,000개가 넘어서, 앞에 붙인
        // 수십 개짜리 후보는 뽑힐 확률이 거의 없어지고 난이도 차이가 사라진다.
        // 우선순위가 다른 풀은 배열째로 나눠 두고 순서대로 시도한다.
        val pools = mutableListOf<List<String>>()
        when (difficulty.similarity) {
            ChallengeSimilarity.CLOSE -> {
                pools += wordsByAuthor(quote.authorId, excluding = quote.slug)
                pools += wordsInCategory(quote.category, excluding = quote.slug)
            }
            ChallengeSimilarity.RELATED ->
                pools += wordsInCategory(quote.category, excluding = quote.slug)
            ChallengeSimilarity.RANDOM -> Unit
        }
        pools += allWords(excluding = quote.slug)

        val distractors = mutableListOf<String>()
        val used = blanked.answers.toMutableSet()
        used += answer

        for (slot in 0 until difficulty.choiceCount - 1) {
            // 두 낱말짜리 문제에서는 한쪽만 바꾼 보기를 섞어 둔다. 훨씬 헷갈린다.
            val replaceOnly: Int? =
                if (blanked.answers.size > 1 && slot % 2 == 0) {
                    random.nextInt(blanked.answers.size)
                } else {
                    null
                }

            val candidate = nextDistractor(
                answers = blanked.answers,
                replaceOnly = replaceOnly,
                pools = pools,
                similarity = difficulty.similarity,
                used = used,
                random = random,
            ) ?: break

            distractors += candidate
            used += candidate
        }

        // 보기를 채우지 못하면 문제를 내지 않는다. 보기가 모자란 문제는 정답이 티가 난다.
        if (distractors.size != difficulty.choiceCount - 1) return null

        val choices = (listOf(answer) + distractors).shuffledWith(random)
        val correctIndex = choices.indexOf(answer)
        if (correctIndex < 0) return null

        return ChallengeQuestion(
            id = "blank:${difficulty.level}:$seed",
            mode = ChallengeMode.FILL_IN_THE_BLANK,
            difficulty = difficulty,
            quote = quote,
            author = author,
            promptText = blanked.text,
            choices = choices,
            correctIndex = correctIndex,
            hint = if (difficulty.showsHint) author.displayName else null,
        )
    }

    /**
     * 오답 하나를 고른다.
     *
     * @param replaceOnly 값이 있으면 그 자리의 낱말만 바꾼 보기를 만든다. 나머지는
     *   정답과 같아서, 한 낱말 차이로 갈리는 보기가 된다.
     */
    private fun nextDistractor(
        answers: List<String>,
        replaceOnly: Int?,
        pools: List<List<String>>,
        similarity: ChallengeSimilarity,
        used: Set<String>,
        random: SeededRandom,
    ): String? {
        // 길이를 맞추는 조건부터 시도하고, 후보가 없으면 조건을 푼다.
        // 길이 조건이 풀보다 바깥에 있는 이유: 같은 인물이 쓴 낱말이라도 길이가
        // 크게 다르면 보기에서 정답이 튀어 보인다. 닮음보다 길이가 먼저다.
        val tolerances: List<Int?> =
            if (similarity == ChallengeSimilarity.CLOSE) listOf(1, 2, null) else listOf(null)

        for (tolerance in tolerances) {
            for (pool in pools) {
                if (pool.isEmpty()) continue
                var attempts = 0
                while (attempts < MAX_ATTEMPTS) {
                    attempts++
                    val word = pool.random(random)

                    val parts: List<String>
                    if (replaceOnly != null && replaceOnly in answers.indices) {
                        // 한 낱말만 바꾼 보기. 나머지는 정답과 같다.
                        if (word == answers[replaceOnly]) continue
                        if (tolerance != null &&
                            kotlin.math.abs(word.length - answers[replaceOnly].length) > tolerance
                        ) continue
                        parts = answers.toMutableList().also { it[replaceOnly] = word }
                    } else {
                        // 전부 새로 뽑는다. 자리마다 서로 다른 낱말이어야 한다.
                        val replaced = mutableListOf<String>()
                        val seen = mutableSetOf<String>()
                        for (original in answers) {
                            val pick = pickWord(
                                pool = pool,
                                avoiding = seen + original,
                                similarTo = if (tolerance == null) null else original,
                                tolerance = tolerance,
                                random = random,
                            ) ?: break
                            seen += pick
                            replaced += pick
                        }
                        if (replaced.size != answers.size) continue
                        parts = replaced
                    }

                    val candidate = parts.joinToString(PAIR_SEPARATOR)
                    if (candidate !in used) return candidate
                }
            }
        }
        return null
    }

    private fun pickWord(
        pool: List<String>,
        avoiding: Set<String>,
        similarTo: String?,
        tolerance: Int?,
        random: SeededRandom,
    ): String? {
        var attempts = 0
        while (attempts < MAX_ATTEMPTS) {
            attempts++
            val word = pool.random(random)
            if (word in avoiding) continue
            if (similarTo != null && tolerance != null &&
                kotlin.math.abs(word.length - similarTo.length) > tolerance
            ) continue
            return word
        }
        return null
    }

    // MARK: 누가 말했을까

    private fun makeAuthorQuestion(
        quote: Quote,
        difficulty: ChallengeDifficulty,
        seed: String,
    ): ChallengeQuestion? {
        val random = SeededRandom("author:${difficulty.level}:$seed")
        val author = library.author(quote)
        if (author.id == QuoteLibrary.unknownAuthor.id) return null

        // 낱말 풀과 같은 이유로 배열을 이어 붙이지 않는다. 전체 인물 백여 명 뒤에
        // 붙인 "같은 시대 · 같은 직업" 몇 명은 뽑힐 확률이 거의 없어진다.
        val others = library.authors.filter {
            it.id != author.id && it.id != QuoteLibrary.unknownAuthor.id
        }
        val pools = mutableListOf<List<Author>>()
        when (difficulty.similarity) {
            ChallengeSimilarity.CLOSE -> {
                pools += others.filter { sharesOccupation(it, author) && sharesEra(it, author) }
                pools += others.filter { sharesOccupation(it, author) }
            }
            ChallengeSimilarity.RELATED ->
                pools += others.filter {
                    it.nationality == author.nationality || sharesOccupation(it, author)
                }
            ChallengeSimilarity.RANDOM -> Unit
        }
        pools += others

        val names = mutableListOf<String>()
        val used = mutableSetOf(author.displayName)
        val needed = difficulty.choiceCount - 1
        for (pool in pools) {
            if (pool.isEmpty()) continue
            var attempts = 0
            while (names.size < needed && attempts < MAX_AUTHOR_ATTEMPTS) {
                attempts++
                val name = pool.random(random).displayName
                if (name in used) continue
                used += name
                names += name
            }
            if (names.size == needed) break
        }
        if (names.size != needed) return null

        val choices = (listOf(author.displayName) + names).shuffledWith(random)
        val correctIndex = choices.indexOf(author.displayName)
        if (correctIndex < 0) return null

        return ChallengeQuestion(
            id = "author:${difficulty.level}:$seed",
            mode = ChallengeMode.GUESS_THE_AUTHOR,
            difficulty = difficulty,
            quote = quote,
            author = author,
            promptText = quote.text,
            choices = choices,
            correctIndex = correctIndex,
            hint = if (difficulty.showsHint) "${author.nationality} · ${author.occupation}" else null,
        )
    }

    /** 직업 문자열("정치인 · 작가")에 겹치는 낱말이 있는지. */
    private fun sharesOccupation(lhs: Author, rhs: Author): Boolean =
        occupationTokens(lhs).intersect(occupationTokens(rhs)).isNotEmpty()

    private fun occupationTokens(author: Author): Set<String> =
        author.occupation.split('·', ',', '/')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    /** 생몰 연도가 겹치거나 가까운지. 연도를 모르면 같은 시대로 보지 않는다. */
    private fun sharesEra(lhs: Author, rhs: Author): Boolean {
        val a = lhs.birthYear ?: return false
        val b = rhs.birthYear ?: return false
        return kotlin.math.abs(a - b) <= ERA_TOLERANCE_YEARS
    }

    // MARK: 낱말 풀

    private fun words(quotes: List<Quote>, excluding: String): List<String> =
        quotes.filter { it.slug != excluding }.flatMap { wordsBySlug[it.slug].orEmpty() }

    private fun allWords(excluding: String): List<String> = words(library.quotes, excluding)

    private fun wordsInCategory(category: AppCategory, excluding: String): List<String> =
        words(library.quotes(category), excluding)

    private fun wordsByAuthor(authorId: String, excluding: String): List<String> =
        words(library.quotes(authorId), excluding)

    companion object {
        /** 한 판의 문제 수. */
        const val QUESTIONS_PER_ROUND = 10

        /** 두 낱말 보기를 이어 붙일 때 쓰는 구분자. */
        const val PAIR_SEPARATOR = " · "

        /** 후보 뽑기를 포기하기까지의 횟수. 풀이 좁아도 무한히 돌지 않게 한다. */
        private const val MAX_ATTEMPTS = 64
        private const val MAX_AUTHOR_ATTEMPTS = 128

        /** 이 정도 안이면 같은 시대로 본다. */
        private const val ERA_TOLERANCE_YEARS = 120
    }
}
