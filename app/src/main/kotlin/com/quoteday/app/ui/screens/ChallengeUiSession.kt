package com.quoteday.app.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quoteday.core.ChallengeDifficulty
import com.quoteday.core.ChallengeQuestion
import com.quoteday.core.ChallengeResult
import com.quoteday.core.ChallengeSession

/**
 * [ChallengeSession] 을 Compose 가 볼 수 있게 감싼 것.
 *
 * `:core` 는 순수 코틀린이라 Compose 의 상태(`mutableStateOf`)를 쓸 수 없다.
 * 그러면 세션 안에서 값이 바뀌어도 화면이 다시 그려지지 않는다 — 보기를 눌러도
 * 아무 일도 일어나지 않는 것처럼 보인다는 뜻이다.
 *
 * 그래서 세션을 건드린 **직후에 값을 그대로 베껴 온다**. 베끼는 코드가 몇 줄
 * 늘지만, 채점 규칙은 `:core` 에 남아 안드로이드 없이 테스트된다.
 */
@Stable
class ChallengeUiSession(private val session: ChallengeSession) {

    val difficulty: ChallengeDifficulty = session.difficulty
    val questionCount: Int = session.questionCount

    var phase: ChallengeSession.Phase by mutableStateOf(session.phase)
        private set
    var index: Int by mutableStateOf(session.index)
        private set
    var displayNumber: Int by mutableStateOf(session.displayNumber)
        private set
    var isLastQuestion: Boolean by mutableStateOf(session.isLastQuestion)
        private set
    var progress: Float by mutableStateOf(session.progress.toFloat())
        private set
    var remainingSeconds: Int? by mutableStateOf(session.remainingSeconds)
        private set
    var wasCorrect: Boolean? by mutableStateOf(session.wasCorrect)
        private set
    var currentQuestion: ChallengeQuestion? by mutableStateOf(session.currentQuestion)
        private set
    var pickedIndex: Int? by mutableStateOf(null)
        private set

    val result: ChallengeResult get() = session.result

    fun isPicked(choiceIndex: Int): Boolean = pickedIndex == choiceIndex

    fun select(choiceIndex: Int) {
        session.select(choiceIndex)
        sync()
    }

    fun advance() {
        session.advance()
        sync()
    }

    fun tick() {
        session.tick()
        sync()
    }

    private fun sync() {
        phase = session.phase
        index = session.index
        displayNumber = session.displayNumber
        isLastQuestion = session.isLastQuestion
        progress = session.progress.toFloat()
        remainingSeconds = session.remainingSeconds
        wasCorrect = session.wasCorrect
        currentQuestion = session.currentQuestion
        // 시간이 지나 끝난 문제에는 고른 보기가 없다. 그래서 세션에서 다시 읽는다.
        pickedIndex = session.questions.getOrNull(session.index)
            ?.choices
            ?.indices
            ?.firstOrNull { session.isPicked(it) }
    }
}
