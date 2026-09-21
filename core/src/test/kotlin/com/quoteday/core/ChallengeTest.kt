package com.quoteday.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 챌린지 문제 생성기 검증. iOS `Tests/ChallengeTests.swift` 와 같은 것을 본다.
 *
 * 화면은 여기서 만든 문제를 그리기만 하므로, 문제가 옳게 만들어지는지만 확인하면
 * 기능 전체가 검증된다. 특히 **보기가 모자란 문제**와 **정답이 두 번 들어간 보기**는
 * 눈으로 찾기 어렵고 사용자에게는 바로 티가 나므로 전수로 확인한다.
 */
class ChallengeTest {

    private val library = QuoteLibrary.shared
    private val generator = ChallengeGenerator(library)

    // MARK: 어절 자르기

    @Test
    fun `tokenizer keeps punctuation outside the word`() {
        val tokens = BlankMaker.tokenize("“인생은 짧다.”")
        assertEquals(2, tokens.size)
        assertEquals("인생은", tokens[0].core)
        assertEquals("“", tokens[0].leading)
        assertEquals("짧다", tokens[1].core)
        assertEquals(".”", tokens[1].trailing)
    }

    @Test
    fun `tokens rebuild the original word`() {
        for (quote in library.quotes) {
            for (token in BlankMaker.tokenize(quote.text)) {
                assertTrue(token.original.isNotEmpty(), "빈 어절이 나왔습니다: ${quote.slug}")
            }
            assertEquals(
                quote.text.split(Regex("\\s+")).filter { it.isNotEmpty() },
                BlankMaker.tokenize(quote.text).map { it.original },
                "어절을 되돌리지 못했습니다: ${quote.slug}",
            )
        }
    }

    @Test
    fun `one letter words are not blank candidates`() {
        // 한 글자 어절은 보기에 늘어놓아도 구분이 안 되고 찍기가 너무 쉽다.
        assertEquals(listOf("정말", "좋아한다"), BlankMaker.candidates("나 는 너 를 정말 좋아한다"))
    }

    // MARK: 결정성

    @Test
    fun `the same seed makes the same round`() {
        val first = generator.makeRound(ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.HARD, "seed-1")
        val second = generator.makeRound(ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.HARD, "seed-1")
        assertEquals(first.map { it.id }, second.map { it.id })
        assertEquals(first.map { it.choices }, second.map { it.choices })
        assertEquals(first.map { it.correctIndex }, second.map { it.correctIndex })
    }

    @Test
    fun `different seeds make different rounds`() {
        val first = generator.makeRound(ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.HARD, "seed-1")
        val second = generator.makeRound(ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.HARD, "seed-2")
        assertNotEquals(first.map { it.id }, second.map { it.id })
    }

    // MARK: 문제의 모양

    /**
     * 모든 모드·단계에서 한 판이 다 채워져야 한다.
     * 문제가 모자라면 판이 짧아지고 점수 비교가 무너진다.
     */
    @Test
    fun `every mode and difficulty fills a full round`() {
        for (mode in ChallengeMode.entries) {
            for (difficulty in ChallengeDifficulty.entries) {
                val round = generator.makeRound(mode, difficulty, "round")
                assertEquals(
                    ChallengeGenerator.QUESTIONS_PER_ROUND,
                    round.size,
                    "${mode.rawValue} ${difficulty.title}에서 문제가 모자랍니다.",
                )
            }
        }
    }

    @Test
    fun `a round never repeats the same quote`() {
        for (mode in ChallengeMode.entries) {
            val slugs = generator.makeRound(mode, ChallengeDifficulty.NORMAL, "unique").map { it.quote.slug }
            assertEquals(slugs.size, slugs.toSet().size, "같은 명언이 한 판에 두 번 나왔습니다.")
        }
    }

    /** 보기 개수, 중복, 정답 위치를 전 명언에 대해 확인한다. */
    @Test
    fun `every generated question is well formed`() {
        for (mode in ChallengeMode.entries) {
            for (difficulty in ChallengeDifficulty.entries) {
                for (quote in generator.sourceQuotes(mode)) {
                    val question = generator.makeQuestion(quote, mode, difficulty, quote.slug) ?: continue

                    assertEquals(
                        difficulty.choiceCount,
                        question.choices.size,
                        "${quote.slug}: 보기 개수가 다릅니다.",
                    )
                    assertEquals(
                        question.choices.size,
                        question.choices.toSet().size,
                        "${quote.slug}: 보기가 중복되었습니다.",
                    )
                    assertTrue(
                        question.correctIndex in question.choices.indices,
                        "${quote.slug}: 정답 위치가 범위를 벗어났습니다.",
                    )
                    assertTrue(
                        question.correctAnswer.isNotEmpty(),
                        "${quote.slug}: 정답이 비어 있습니다.",
                    )
                }
            }
        }
    }

    @Test
    fun `blank count matches the difficulty`() {
        for (difficulty in ChallengeDifficulty.entries) {
            for (quote in generator.sourceQuotes(ChallengeMode.FILL_IN_THE_BLANK).take(40)) {
                val question = generator.makeQuestion(
                    quote, ChallengeMode.FILL_IN_THE_BLANK, difficulty, quote.slug,
                ) ?: continue

                val blanks = question.promptText.split(ChallengeQuestion.BLANK_MARKER).size - 1
                assertEquals(
                    difficulty.blankCount,
                    blanks,
                    "${quote.slug} ${difficulty.title}: 빈칸 수가 다릅니다.",
                )
            }
        }
    }

    /** 정답 낱말이 지문에 그대로 남아 있으면 문제가 성립하지 않는다. */
    @Test
    fun `the answer is removed from the prompt`() {
        for (quote in generator.sourceQuotes(ChallengeMode.FILL_IN_THE_BLANK)) {
            val question = generator.makeQuestion(
                quote, ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.BEGINNER, quote.slug,
            ) ?: continue
            assertTrue(
                question.promptText.contains(ChallengeQuestion.BLANK_MARKER),
                "${quote.slug}: 빈칸이 뚫리지 않았습니다.",
            )
        }
    }

    // MARK: 힌트와 제한 시간

    @Test
    fun `hint only appears in the easy levels`() {
        for (difficulty in ChallengeDifficulty.entries) {
            val question = generator.makeRound(
                ChallengeMode.GUESS_THE_AUTHOR, difficulty, "hint", count = 1,
            ).firstOrNull()
            assertNotNull(question, "${difficulty.title}에서 문제를 만들지 못했습니다.")
            assertEquals(
                difficulty.showsHint,
                question.hint != null,
                "${difficulty.title}의 힌트 노출이 정의와 다릅니다.",
            )
        }
    }

    @Test
    fun `time limit only applies to the top two levels`() {
        assertNull(ChallengeDifficulty.BEGINNER.timeLimitSeconds)
        assertNull(ChallengeDifficulty.NORMAL.timeLimitSeconds)
        assertNull(ChallengeDifficulty.HARD.timeLimitSeconds)
        assertNotNull(ChallengeDifficulty.VERY_HARD.timeLimitSeconds)
        assertNotNull(ChallengeDifficulty.EXTREME.timeLimitSeconds)
        // 위 단계가 더 촉박해야 한다.
        assertTrue(
            ChallengeDifficulty.EXTREME.timeLimitSeconds!! < ChallengeDifficulty.VERY_HARD.timeLimitSeconds!!
        )
    }

    /** 단계가 오를수록 보기와 빈칸이 줄어들지 않아야 한다. */
    @Test
    fun `difficulty knobs increase monotonically`() {
        val levels = ChallengeDifficulty.entries.sortedBy { it.level }
        for ((lower, higher) in levels.zipWithNext()) {
            assertTrue(lower.choiceCount <= higher.choiceCount, "${lower.title} → ${higher.title}")
            assertTrue(lower.blankCount <= higher.blankCount, "${lower.title} → ${higher.title}")
            assertTrue(lower.pointsPerQuestion < higher.pointsPerQuestion, "${lower.title} → ${higher.title}")
        }
    }

    // MARK: 귀속

    /**
     * 출처가 확인되지 않은 명언으로 "누가 말했을까"를 내면 앱이 확인되지 않은
     * 귀속을 정답이라고 가르치게 된다.
     */
    @Test
    fun `disputed quotes are excluded from the author mode`() {
        for (quote in generator.sourceQuotes(ChallengeMode.GUESS_THE_AUTHOR)) {
            assertFalse(
                library.isDisputed(quote.slug),
                "귀속이 확인되지 않은 명언이 인물 문제로 나옵니다: ${quote.slug}",
            )
        }
    }

    /** 빈칸 채우기는 인물을 묻지 않으므로 그대로 쓴다. */
    @Test
    fun `disputed quotes still appear in the blank mode`() {
        assertTrue(
            generator.sourceQuotes(ChallengeMode.FILL_IN_THE_BLANK).any { library.isDisputed(it.slug) },
            "인물을 묻지 않는 문제까지 뺄 이유는 없습니다.",
        )
    }

    // MARK: 한 판 진행

    @Test
    fun `session scores and streaks`() {
        val session = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.NORMAL, generator, seed = "session",
        )
        assertEquals(ChallengeGenerator.QUESTIONS_PER_ROUND, session.questionCount)

        // 앞의 셋은 맞히고 넷째는 틀린다.
        repeat(3) {
            session.select(session.currentQuestion!!.correctIndex)
            assertEquals(true, session.wasCorrect)
            session.advance()
        }
        assertEquals(3, session.correctCount)
        assertEquals(3, session.currentStreak)

        val question = session.currentQuestion!!
        session.select(question.choices.indices.first { it != question.correctIndex })
        assertEquals(false, session.wasCorrect)
        assertEquals(0, session.currentStreak)
        assertEquals(3, session.bestStreak)
        assertEquals(3, session.correctCount)
    }

    @Test
    fun `answering twice is ignored`() {
        val session = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.NORMAL, generator, seed = "twice",
        )
        val correct = session.currentQuestion!!.correctIndex
        session.select(correct)
        session.select(correct)
        assertEquals(1, session.correctCount)
        assertEquals(ChallengeSession.Phase.REVEALING, session.phase)
    }

    @Test
    fun `running out of time counts as wrong`() {
        val session = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.EXTREME, generator, seed = "timeout",
        )
        val limit = ChallengeDifficulty.EXTREME.timeLimitSeconds!!
        repeat(limit) { session.tick() }

        assertEquals(0, session.remainingSeconds)
        assertEquals(ChallengeSession.Phase.REVEALING, session.phase)
        assertEquals(false, session.wasCorrect)
        assertEquals(0, session.correctCount)
        // 시간이 지나 끝난 문제에는 고른 보기가 없다.
        assertTrue(session.questions[0].choices.indices.none { session.isPicked(it) })
    }

    @Test
    fun `tick does nothing without a time limit`() {
        val session = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.BEGINNER, generator, seed = "no-timer",
        )
        repeat(100) { session.tick() }
        assertNull(session.remainingSeconds)
        assertEquals(ChallengeSession.Phase.ASKING, session.phase)
    }

    @Test
    fun `advancing past the last question finishes the round`() {
        val session = ChallengeSession.start(
            ChallengeMode.GUESS_THE_AUTHOR, ChallengeDifficulty.BEGINNER, generator, seed = "finish",
        )
        repeat(session.questionCount) {
            session.select(session.currentQuestion!!.correctIndex)
            session.advance()
        }
        assertEquals(ChallengeSession.Phase.FINISHED, session.phase)
        assertTrue(session.result.isPerfect)
        assertEquals("전부 맞혔습니다", session.result.headline)
        assertEquals(
            ChallengeDifficulty.BEGINNER.perfectScore,
            session.result.points,
            "다 맞힌 판의 점수가 만점과 다릅니다.",
        )
    }

    /** 같은 단계를 두 번째 골라도 새 문제가 나와야 한다. */
    @Test
    fun `playing again gives different questions`() {
        val first = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.NORMAL, generator,
        )
        val second = ChallengeSession.start(
            ChallengeMode.FILL_IN_THE_BLANK, ChallengeDifficulty.NORMAL, generator,
        )
        assertNotEquals(
            first.questions.map { it.quote.slug },
            second.questions.map { it.quote.slug },
        )
    }

    // MARK: 점수 구간

    @Test
    fun `rank buckets cover the whole score range`() {
        assertEquals(0, RankBucket.index(0))
        assertEquals(0, RankBucket.index(-50))
        assertEquals(RankBucket.count - 1, RankBucket.index(ChallengeScore.maximumTotal))
        assertEquals(RankBucket.count - 1, RankBucket.index(ChallengeScore.maximumTotal * 10))
        assertTrue(RankBucket.count > 1)
    }

    @Test
    fun `the maximum total is the sum of every mode and difficulty`() {
        val total = ChallengeScore.total { _, _ -> ChallengeGenerator.QUESTIONS_PER_ROUND }
        assertEquals(ChallengeScore.maximumTotal, total)
    }
}
