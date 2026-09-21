package com.quoteday.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quoteday.app.QuoteDayApplication
import com.quoteday.app.ui.ClayCard
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.QuoteCard
import com.quoteday.app.ui.QuoteCardStyle
import com.quoteday.app.ui.TAB_BAR_INSET
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 앱을 열면 바로 오늘의 명언이 보이는 화면.
 *
 * 오늘의 명언은 날짜에서 결정적으로 정해진다. 새로고침이 없는 것은 그래서다 —
 * 같은 날에는 몇 번을 열어도 같은 문장이 나온다.
 */
@Composable
fun HomeScreen(
    app: QuoteDayApplication,
    onOpenQuote: (String) -> Unit,
    onOpenQuotes: () -> Unit,
    onOpenSchedules: () -> Unit,
) {
    val colors = ClayTheme.colors
    val today = remember { LocalDate.now() }
    val presentation = remember(today, app.settings.preferredCategory) {
        app.quotes.presentationOfTheDay(today, app.settings.preferredCategory)
    }
    val nearby = remember(presentation.quote.slug) {
        app.library.quotes(presentation.quote.category)
            .filter { it.slug != presentation.quote.slug }
            .take(5)
            .map { app.library.presentation(it) }
    }

    LaunchedEffect(presentation.quote.slug) {
        app.hearts.refresh(listOf(presentation.quote.slug) + nearby.map { it.quote.slug })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = ClaySpacing.m,
            end = ClaySpacing.m,
            top = ClaySpacing.m,
            bottom = TAB_BAR_INSET,
        ),
        verticalArrangement = Arrangement.spacedBy(ClaySpacing.m),
    ) {
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    greeting(LocalTime.now()),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Text(
                    today.format(dateFormatter),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
            }
        }

        item {
            QuoteCard(
                presentation = presentation,
                style = QuoteCardStyle.HERO,
                heartCount = app.hearts.count(presentation.quote.slug),
                isHearted = app.hearts.isHearted(presentation.quote.slug),
                onHeart = { app.hearts.toggle(presentation.quote.slug) },
                onClick = { onOpenQuote(presentation.quote.slug) },
            )
        }

        item {
            // 다음 일정. 없으면 카드를 그리지 않는다 — 빈 카드는 자리만 먹는다.
            val next = app.schedules.nextOccurrence()
            if (next != null) {
                ClayCard(onClick = onOpenSchedules) {
                    Column(
                        Modifier.fillMaxWidth().padding(ClaySpacing.m),
                        verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                    ) {
                        Text(
                            "다음 일정",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textSecondary,
                        )
                        Text(
                            next.displayTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Text(
                            "${next.start.format(nextScheduleFormatter)} · ${next.category.displayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
        }

        item {
            Text(
                "${presentation.quote.category.title} 이야기 더 보기",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = ClaySpacing.s),
            )
        }

        items(nearby, key = { it.quote.slug }) { item ->
            QuoteCard(
                presentation = item,
                heartCount = app.hearts.count(item.quote.slug),
                isHearted = app.hearts.isHearted(item.quote.slug),
                onHeart = { app.hearts.toggle(item.quote.slug) },
                onClick = { onOpenQuote(item.quote.slug) },
            )
        }

        item {
            ClayCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenQuotes) {
                Text(
                    "명언 ${app.library.count}편 모두 보기",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                    modifier = Modifier.fillMaxWidth().padding(ClaySpacing.m),
                )
            }
        }
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

private val nextScheduleFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)

internal fun greeting(time: LocalTime): String = when (time.hour) {
    in 5..10 -> "좋은 아침이에요"
    in 11..13 -> "점심 무렵이에요"
    in 14..17 -> "오후를 보내고 있어요"
    in 18..21 -> "저녁이에요"
    else -> "늦은 밤이에요"
}
