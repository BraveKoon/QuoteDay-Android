package com.quoteday.core

/** 문제 유형. */
enum class ChallengeMode(val rawValue: String) {
    FILL_IN_THE_BLANK("fillInTheBlank"),
    GUESS_THE_AUTHOR("guessTheAuthor");

    val title: String
        get() = when (this) {
            FILL_IN_THE_BLANK -> "빈칸 채우기"
            GUESS_THE_AUTHOR -> "누가 말했을까"
        }

    val summary: String
        get() = when (this) {
            FILL_IN_THE_BLANK -> "명언에서 사라진 낱말을 찾아 넣습니다."
            GUESS_THE_AUTHOR -> "문장만 보고 말한 사람을 고릅니다."
        }

    companion object {
        fun from(rawValue: String): ChallengeMode =
            entries.firstOrNull { it.rawValue == rawValue } ?: FILL_IN_THE_BLANK
    }
}

/** 오답을 정답과 얼마나 닮게 만들지. */
enum class ChallengeSimilarity { RANDOM, RELATED, CLOSE }

/**
 * 난이도. 올라갈수록 보기가 늘고, 힌트가 사라지고, 오답이 정답과 비슷해진다.
 * `level` 은 저장되는 값이므로 iOS 와 같아야 한다.
 */
enum class ChallengeDifficulty(val level: Int) {
    BEGINNER(1), NORMAL(2), HARD(3), VERY_HARD(4), EXTREME(5);

    val title: String
        get() = when (this) {
            BEGINNER -> "입문"; NORMAL -> "보통"; HARD -> "어려움"
            VERY_HARD -> "매우 어려움"; EXTREME -> "극한"
        }

    val choiceCount: Int
        get() = when (this) {
            BEGINNER -> 3; NORMAL -> 4; HARD -> 4; VERY_HARD -> 5; EXTREME -> 6
        }

    val blankCount: Int
        get() = when (this) {
            BEGINNER, NORMAL, HARD -> 1
            VERY_HARD, EXTREME -> 2
        }

    val showsHint: Boolean get() = this == BEGINNER || this == NORMAL

    val similarity: ChallengeSimilarity
        get() = when (this) {
            BEGINNER -> ChallengeSimilarity.RANDOM
            NORMAL -> ChallengeSimilarity.RELATED
            HARD, VERY_HARD, EXTREME -> ChallengeSimilarity.CLOSE
        }

    val pointsPerQuestion: Int
        get() = when (this) {
            BEGINNER -> 10; NORMAL -> 20; HARD -> 35; VERY_HARD -> 55; EXTREME -> 80
        }

    val perfectScore: Int get() = pointsPerQuestion * ChallengeGenerator.QUESTIONS_PER_ROUND

    /** 제한 시간(초). 없으면 null. */
    val timeLimitSeconds: Int?
        get() = when (this) {
            BEGINNER, NORMAL, HARD -> null
            VERY_HARD -> 20
            EXTREME -> 15
        }

    fun detail(mode: ChallengeMode): String {
        val parts = mutableListOf("문제당 ${pointsPerQuestion}점", "보기 ${choiceCount}개")
        if (mode == ChallengeMode.FILL_IN_THE_BLANK && blankCount > 1) parts += "빈칸 ${blankCount}개"
        parts += if (showsHint) "힌트 있음" else "힌트 없음"
        timeLimitSeconds?.let { parts += "${it}초" }
        return parts.joinToString(" · ")
    }

    companion object {
        fun from(level: Int): ChallengeDifficulty =
            entries.firstOrNull { it.level == level } ?: BEGINNER
    }
}

/** 문제 하나. */
data class ChallengeQuestion(
    val id: String,
    val mode: ChallengeMode,
    val difficulty: ChallengeDifficulty,
    val quote: Quote,
    val author: Author,
    /** 화면에 띄울 본문. 빈칸 채우기에서는 낱말이 [BLANK_MARKER] 로 바뀌어 있다. */
    val promptText: String,
    /** 보기. 순서는 이미 섞여 있다. */
    val choices: List<String>,
    val correctIndex: Int,
    val hint: String? = null,
) {
    val correctAnswer: String get() = choices[correctIndex]

    fun isCorrect(index: Int): Boolean = index == correctIndex

    /** 정답을 채워 넣은 원래 문장. 해설에서 보여 준다. */
    val revealedText: String
        get() = if (mode == ChallengeMode.FILL_IN_THE_BLANK) quote.text else quote.text

    companion object {
        const val BLANK_MARKER = "____"
    }
}

/** 한 문제에 대한 답. */
sealed interface ChallengeAnswer {
    data class Picked(val index: Int) : ChallengeAnswer

    /** 제한 시간을 넘겼다. 오답으로 친다. */
    data object TimedOut : ChallengeAnswer

    val pickedIndex: Int? get() = (this as? Picked)?.index
}

/** 한 판의 결과. */
data class ChallengeResult(
    val mode: ChallengeMode,
    val difficulty: ChallengeDifficulty,
    val correctCount: Int,
    val questionCount: Int,
    val bestStreak: Int,
    /** 이번 판에서 최고 기록을 새로 썼는지. */
    val isNewRecord: Boolean,
) {
    val accuracy: Double get() = if (questionCount > 0) correctCount.toDouble() / questionCount else 0.0

    /** 모두 맞혔는지. */
    val isPerfect: Boolean get() = questionCount > 0 && correctCount == questionCount

    val points: Int get() = ChallengeScore.points(correctCount, difficulty)

    /** 결과 화면 맨 위에 띄우는 한마디. */
    val headline: String
        get() = when {
            accuracy >= 1.0 -> "전부 맞혔습니다"
            accuracy >= 0.8 -> "거의 다 맞혔습니다"
            accuracy >= 0.5 -> "절반은 넘겼습니다"
            accuracy >= 0.2 -> "다음 판이 있습니다"
            else -> "한 단계 낮춰 볼까요"
        }
}

/** 배점과 랭킹 구간. */
object ChallengeScore {
    fun points(correctCount: Int, difficulty: ChallengeDifficulty): Int =
        maxOf(0, correctCount) * difficulty.pointsPerQuestion

    /** 모드·난이도별 최고 정답 수를 받아 랭킹 총점을 낸다. */
    fun total(bestScore: (ChallengeMode, ChallengeDifficulty) -> Int): Int {
        var sum = 0
        for (mode in ChallengeMode.entries) {
            for (difficulty in ChallengeDifficulty.entries) {
                sum += points(bestScore(mode, difficulty), difficulty)
            }
        }
        return sum
    }

    /** 모든 모드·난이도를 다 맞혔을 때의 총점. */
    val maximumTotal: Int =
        ChallengeMode.entries.size * ChallengeDifficulty.entries.sumOf { it.perfectScore }
}

/**
 * 점수 분포를 세는 구간.
 *
 * 사람마다 점수 레코드를 두고 세려면 조건 검색이 필요하다. 분포를 미리 구간으로
 * 나눠 세어 두면 ID 로 한 번에 가져올 수 있다. iOS 와 같은 폭을 써야 두 플랫폼의
 * 점수를 나중에 합칠 수 있다.
 */
object RankBucket {
    const val WIDTH = 200
    val count: Int = (ChallengeScore.maximumTotal / WIDTH) + 1
    fun index(total: Int): Int = minOf(maxOf(0, total) / WIDTH, count - 1)
    val allIndices: List<Int> get() = (0 until count).toList()
}
