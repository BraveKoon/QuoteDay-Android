package com.quoteday.core

/**
 * 문장에서 낱말을 뽑아 빈칸으로 바꾼다.
 *
 * 한국어는 조사가 붙어 다녀서 형태소 단위로 자르려면 사전이 필요하다. 여기서는
 * **어절**(띄어쓰기 단위)을 그대로 하나의 낱말로 쓴다. "인생은"에서 "인생"만
 * 뽑아내지 않고 "인생은" 통째로 뚫는다는 뜻인데, 문제로서는 오히려 이쪽이
 * 자연스럽다 — 조사가 남아 있으면 답이 절반쯤 보인다.
 *
 * 앞뒤에 붙은 따옴표·마침표는 빈칸 밖에 남긴다. 문장 부호까지 지우면 남은 문장이
 * 어색해지고 보기에 마침표가 섞여 들어간다.
 */
object BlankMaker {

    /** 어절 하나. `leading + core + trailing` 이 원래 어절이다. */
    data class Token(val leading: String, val core: String, val trailing: String) {
        val original: String get() = leading + core + trailing
    }

    /** 빈칸을 뚫은 결과. */
    data class Blanked(val text: String, val answers: List<String>)

    /** 낱말 앞뒤에서 떼어 내는 문자. iOS 와 같은 집합이다. */
    private val punctuation: Set<Char> = (
        ".,!?;:'\"()[]{}" +
            "“”‘’「」『』" +
            "—–…·，．！？―-"
        ).toSet()

    fun tokenize(text: String): List<Token> =
        text.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { piece ->
            var start = 0
            var end = piece.length
            while (start < end && piece[start] in punctuation) start++
            while (end > start && piece[end - 1] in punctuation) end--
            Token(
                leading = piece.substring(0, start),
                core = piece.substring(start, end),
                trailing = piece.substring(end),
            )
        }

    /**
     * 빈칸으로 뚫을 수 있는 낱말.
     *
     * 한 글자 어절("나", "그")은 뺀다. 보기로 늘어놓아도 구분이 안 되고 문맥만으로
     * 찍기가 너무 쉽다. 글자가 하나도 없는 어절(숫자·기호만)도 뺀다.
     */
    fun candidates(text: String): List<String> = tokenize(text).map { it.core }.filter(::isUsable)

    private fun isUsable(core: String): Boolean =
        core.length >= 2 && core.any { it.isLetter() }

    /** 남겨 둘 최소 어절 수. 이보다 짧아지면 문장이 문제 구실을 못 한다. */
    private const val MINIMUM_REMAINING_TOKENS = 3

    /**
     * @param preferLongWords 참이면 긴 낱말 쪽에서 고른다. 긴 어절일수록 문맥으로
     *   유추하기 어려워 문제가 어려워진다.
     * @return 조건을 채우지 못하면 null. 부르는 쪽은 그 명언을 건너뛴다.
     */
    fun make(
        text: String,
        blankCount: Int,
        preferLongWords: Boolean,
        random: SeededRandom,
    ): Blanked? {
        if (blankCount <= 0) return null
        val tokens = tokenize(text)
        val candidateIndices = tokens.indices.filter { isUsable(tokens[it].core) }

        if (candidateIndices.size < blankCount) return null
        if (tokens.size - blankCount < MINIMUM_REMAINING_TOKENS) return null

        // 긴 낱말 우선일 때는 상위 절반만 후보로 남긴다. 길이가 같으면 인덱스로
        // 순서를 못 박아, 정렬이 불안정해도 결과가 흔들리지 않게 한다.
        var pickPool = candidateIndices
        if (preferLongWords) {
            val ranked = candidateIndices.sortedWith(
                compareByDescending<Int> { tokens[it].core.length }.thenBy { it }
            )
            pickPool = ranked.take(maxOf(blankCount, (ranked.size + 1) / 2))
        }

        val shuffled = pickPool.shuffledWith(random)
        val chosen = mutableListOf<Int>()

        // 빈칸이 붙어 있으면 문맥이 통째로 사라진다. 먼저 떨어뜨려 고른다.
        for (index in shuffled) {
            if (chosen.size >= blankCount) break
            if (chosen.all { kotlin.math.abs(it - index) >= 2 }) chosen += index
        }
        // 짧은 문장이라 떨어뜨릴 수 없으면 조건을 푼다.
        for (index in shuffled) {
            if (chosen.size >= blankCount) break
            if (index !in chosen) chosen += index
        }
        if (chosen.size != blankCount) return null

        chosen.sort()
        val blanks = chosen.toSet()
        val rendered = tokens.indices.map { index ->
            if (index in blanks) {
                tokens[index].leading + ChallengeQuestion.BLANK_MARKER + tokens[index].trailing
            } else {
                tokens[index].original
            }
        }

        return Blanked(
            text = rendered.joinToString(" "),
            answers = chosen.map { tokens[it].core },
        )
    }
}
