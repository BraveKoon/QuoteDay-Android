package com.quoteday.core

/**
 * 진행 중인 한 판.
 *
 * 문제 생성은 [ChallengeGenerator] 가 이미 끝내 놓았고, 여기서는 진행과 채점만 한다.
 * 화면은 상태를 읽고 [select] · [advance] · [tick] 세 개만 부른다.
 *
 * 안드로이드 화면(Compose)은 이 객체를 직접 들고 있지 않고 ViewModel 이 감싼다.
 * 그래야 이 클래스가 순수한 코틀린으로 남아 `:core` 에서 테스트된다 — 안드로이드
 * SDK 가 없는 곳에서도 채점 규칙을 검증할 수 있다는 뜻이다.
 */
class ChallengeSession(
    val mode: ChallengeMode,
    val difficulty: ChallengeDifficulty,
    val questions: List<ChallengeQuestion>,
) {
    enum class Phase {
        /** 문제를 풀고 있다. */
        ASKING,

        /** 답을 냈고 정답을 보여 주는 중이다. */
        REVEALING,

        /** 판이 끝났다. */
        FINISHED,
    }

    var index: Int = 0
        private set
    var answer: ChallengeAnswer? = null
        private set
    var correctCount: Int = 0
        private set
    var currentStreak: Int = 0
        private set
    var bestStreak: Int = 0
        private set
    var phase: Phase = if (questions.isEmpty()) Phase.FINISHED else Phase.ASKING
        private set

    /** 제한 시간이 있는 단계에서 남은 초. 없으면 null. */
    var remainingSeconds: Int? = difficulty.timeLimitSeconds
        private set

    // MARK: 읽기

    val currentQuestion: ChallengeQuestion? get() = questions.getOrNull(index)

    val questionCount: Int get() = questions.size

    /** 1부터 세는 문제 번호. 화면에 "3 / 10" 으로 쓴다. */
    val displayNumber: Int get() = minOf(index + 1, questionCount)

    val isLastQuestion: Boolean get() = index >= questionCount - 1

    val progress: Double
        get() = if (questionCount > 0) index.toDouble() / questionCount else 0.0

    /** 이번 문제를 맞혔는지. 아직 답하지 않았으면 null. */
    val wasCorrect: Boolean?
        get() {
            val answer = answer ?: return null
            val question = currentQuestion ?: return null
            val picked = answer.pickedIndex ?: return false
            return question.isCorrect(picked)
        }

    /** 이 보기를 사용자가 골랐는지. 시간이 지나 끝난 문제에는 고른 보기가 없다. */
    fun isPicked(choiceIndex: Int): Boolean = answer?.pickedIndex == choiceIndex

    /** 기록 갱신 여부는 저장소가 판단한다. 여기서는 자리만 채운다. */
    val result: ChallengeResult
        get() = ChallengeResult(
            mode = mode,
            difficulty = difficulty,
            correctCount = correctCount,
            questionCount = questionCount,
            bestStreak = bestStreak,
            isNewRecord = false,
        )

    // MARK: 진행

    /** 보기를 골랐다. 이미 답한 문제면 아무 일도 하지 않는다. */
    fun select(choiceIndex: Int) {
        if (phase != Phase.ASKING) return
        val question = currentQuestion ?: return
        if (choiceIndex !in question.choices.indices) return
        record(ChallengeAnswer.Picked(choiceIndex))
    }

    /** 다음 문제로 넘어간다. 마지막 문제였으면 판이 끝난다. */
    fun advance() {
        if (phase != Phase.REVEALING) return
        if (isLastQuestion) {
            phase = Phase.FINISHED
            return
        }
        index += 1
        answer = null
        phase = Phase.ASKING
        remainingSeconds = difficulty.timeLimitSeconds
    }

    /** 1초마다 화면에서 불러 준다. 제한 시간이 없으면 아무 일도 하지 않는다. */
    fun tick() {
        if (phase != Phase.ASKING) return
        val remaining = remainingSeconds ?: return
        if (remaining <= 1) {
            remainingSeconds = 0
            record(ChallengeAnswer.TimedOut)
        } else {
            remainingSeconds = remaining - 1
        }
    }

    private fun record(answer: ChallengeAnswer) {
        val question = currentQuestion ?: return
        this.answer = answer
        phase = Phase.REVEALING

        val isCorrect = answer.pickedIndex?.let(question::isCorrect) ?: false
        if (isCorrect) {
            correctCount += 1
            currentStreak += 1
            bestStreak = maxOf(bestStreak, currentStreak)
        } else {
            currentStreak = 0
        }
    }

    companion object {
        /**
         * 명언 라이브러리에서 새 판을 만든다.
         *
         * seed 를 넘기지 않으면 매번 다른 문제가 나온다 — 같은 단계를 두 번째
         * 골라도 새 문제가 나와야 한다는 뜻이다.
         */
        fun start(
            mode: ChallengeMode,
            difficulty: ChallengeDifficulty,
            generator: ChallengeGenerator = ChallengeGenerator(),
            seed: String = java.util.UUID.randomUUID().toString(),
        ): ChallengeSession = ChallengeSession(
            mode = mode,
            difficulty = difficulty,
            questions = generator.makeRound(mode = mode, difficulty = difficulty, seed = seed),
        )
    }
}
