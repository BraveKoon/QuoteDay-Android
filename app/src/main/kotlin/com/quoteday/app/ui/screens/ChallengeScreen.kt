package com.quoteday.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.ClayCard
import com.quoteday.app.ui.ClayChip
import com.quoteday.app.ui.ClayRadius
import com.quoteday.app.ui.ClayScreenHeader
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.TAB_BAR_INSET
import com.quoteday.core.ChallengeDifficulty
import com.quoteday.core.ChallengeMode
import com.quoteday.core.ChallengeResult
import com.quoteday.core.ChallengeScore
import com.quoteday.core.ChallengeSession
import kotlinx.coroutines.delay

/**
 * 챌린지.
 *
 * 화면 하나가 세 모습을 갖는다 — 고르는 중 / 푸는 중 / 결과. 화면을 나누지 않는
 * 이유는 한 판이 끝나면 곧바로 다음 판으로 돌아와야 하고, 그 사이에 백스택이
 * 끼면 "한 판 더"가 어색해지기 때문이다.
 */
@Composable
fun ChallengeScreen(app: QuoteDayApplication) {
    var session by remember { mutableStateOf<ChallengeUiSession?>(null) }
    var finished by remember { mutableStateOf<ChallengeResult?>(null) }

    val current = session
    val result = finished

    when {
        result != null -> ChallengeResultView(
            result = result,
            onPlayAgain = {
                // seed 를 넘기지 않으므로 같은 단계라도 문제가 새로 뽑힌다.
                session = ChallengeUiSession(
                    ChallengeSession.start(result.mode, result.difficulty, app.generator)
                )
                finished = null
            },
            onClose = { finished = null; session = null },
        )

        current != null -> ChallengeQuizView(
            session = current,
            onFinish = {
                finished = app.challengeRecords.record(current.result)
                session = null
            },
        )

        else -> ChallengeHomeView(
            app = app,
            onStart = { mode, difficulty ->
                session = ChallengeUiSession(ChallengeSession.start(mode, difficulty, app.generator))
            },
        )
    }
}

// ---------------------------------------------------------------- 고르는 중

@Composable
private fun ChallengeHomeView(
    app: QuoteDayApplication,
    onStart: (ChallengeMode, ChallengeDifficulty) -> Unit,
) {
    val colors = ClayTheme.colors
    var mode by remember { mutableStateOf(ChallengeMode.FILL_IN_THE_BLANK) }
    val records = app.challengeRecords

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = ClaySpacing.m, end = ClaySpacing.m, top = ClaySpacing.m, bottom = TAB_BAR_INSET,
        ),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
    ) {
        item {
            ClayScreenHeader(
                title = "챌린지",
                subtitle = "총점 ${records.total}점 / ${ChallengeScore.maximumTotal}점",
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = ClaySpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
            ) {
                for (item in ChallengeMode.entries) {
                    val isOn = item == mode
                    ClayChip(
                        text = item.title,
                        tint = if (isOn) colors.accent else colors.surfaceRaised,
                        textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
                        onClick = { mode = item },
                    )
                }
            }
        }

        item {
            Text(
                mode.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = ClaySpacing.xs),
            )
        }

        for (difficulty in ChallengeDifficulty.entries) {
            item(key = "${mode.rawValue}:${difficulty.level}") {
                ClayCard(onClick = { onStart(mode, difficulty) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(ClaySpacing.m),
                        verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                difficulty.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                bestText(records.best(mode, difficulty), records.hasPlayed(mode, difficulty)),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.accent,
                            )
                        }
                        Text(
                            difficulty.detail(mode),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private fun bestText(best: Int, hasPlayed: Boolean): String =
    if (hasPlayed) "최고 ${best}문제" else "아직 안 함"

// ---------------------------------------------------------------- 푸는 중

@Composable
private fun ChallengeQuizView(
    session: ChallengeUiSession,
    onFinish: () -> Unit,
) {
    val colors = ClayTheme.colors

    // 1초마다 남은 시간을 깎는다. 제한 시간이 없는 단계에서는 tick 이 아무 일도 하지 않는다.
    LaunchedEffect(session, session.index) {
        if (session.difficulty.timeLimitSeconds == null) return@LaunchedEffect
        while (session.phase == ChallengeSession.Phase.ASKING) {
            delay(1_000)
            session.tick()
        }
    }

    LaunchedEffect(session.phase) {
        if (session.phase == ChallengeSession.Phase.FINISHED) onFinish()
    }

    val question = session.currentQuestion ?: return

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = ClaySpacing.m, vertical = ClaySpacing.m),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.m),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${session.displayNumber} / ${session.questionCount}",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            session.remainingSeconds?.let { remaining ->
                Text(
                    "${remaining}초",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (remaining <= 5) colors.danger else colors.textSecondary,
                )
            }
        }

        LinearProgressIndicator(
            progress = { session.progress },
            modifier = Modifier.fillMaxWidth(),
            color = colors.accent,
            trackColor = colors.surfaceSunken,
        )

        ClayCard {
            Column(
                Modifier.fillMaxWidth().padding(ClaySpacing.l),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
            ) {
                Text(
                    question.promptText,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                question.hint?.let {
                    Text(
                        "힌트 · $it",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
            question.choices.forEachIndexed { index, choice ->
                val revealing = session.phase != ChallengeSession.Phase.ASKING
                val background = when {
                    !revealing -> colors.surface
                    index == question.correctIndex -> colors.tint(question.quote.category)
                    session.isPicked(index) -> colors.danger
                    else -> colors.surface
                }
                ClayCard(
                    background = background,
                    cornerRadius = ClayRadius.control,
                    onClick = { session.select(index) },
                ) {
                    Text(
                        choice,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (revealing && background != colors.surface) {
                            colors.textOnTint
                        } else {
                            colors.textPrimary
                        },
                        modifier = Modifier.fillMaxWidth().padding(ClaySpacing.m),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
            if (session.phase == ChallengeSession.Phase.REVEALING) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs)) {
                    Text(
                        if (session.wasCorrect == true) "맞혔습니다" else "정답: ${question.correctAnswer}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (session.wasCorrect == true) colors.accent else colors.danger,
                    )
                    Text(
                        "— ${question.author.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textSecondary,
                    )
                    PrimaryButton(
                        text = if (session.isLastQuestion) "결과 보기" else "다음 문제",
                        onClick = { session.advance() },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 결과

@Composable
private fun ChallengeResultView(
    result: ChallengeResult,
    onPlayAgain: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = ClayTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = ClaySpacing.m, vertical = ClaySpacing.m),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.m),
    ) {
        ClayCard(cornerRadius = ClayRadius.hero) {
            Column(
                Modifier.fillMaxWidth().padding(ClaySpacing.l),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(result.headline, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text(
                    "${result.correctCount} / ${result.questionCount} 문제 · ${result.points}점",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                )
                Text(
                    "최고 연속 ${result.bestStreak}문제",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                if (result.isNewRecord) {
                    Text(
                        "기록을 새로 썼습니다",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                        modifier = Modifier.padding(top = ClaySpacing.xs),
                    )
                }
            }
        }

        PrimaryButton(text = "한 판 더", onClick = onPlayAgain)
        PrimaryButton(text = "단계 고르기", onClick = onClose, isQuiet = true)
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isQuiet: Boolean = false,
) {
    val colors = ClayTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        shape = RoundedCornerShape(ClayRadius.control),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isQuiet) colors.surfaceRaised else colors.accent,
            contentColor = if (isQuiet) colors.textPrimary else colors.textOnAccent,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
