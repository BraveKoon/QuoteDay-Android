package com.quoteday.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.quoteday.app.ui.ClayEmptyState
import com.quoteday.app.ui.ClayRadius
import com.quoteday.app.ui.ClayScreenHeader
import com.quoteday.app.ui.ClaySpacing
import com.quoteday.app.ui.ClayTheme
import com.quoteday.app.ui.TAB_BAR_INSET
import com.quoteday.core.Schedule
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 일정.
 *
 * 날짜를 하나 고르고 그날의 회차를 본다. 반복 일정은 저장된 행이 한 건뿐이고
 * 회차는 계산해서 나오므로, 화면은 둘을 구분하지 않는다.
 */
@Composable
fun ScheduleScreen(
    app: QuoteDayApplication,
    onOpenQuote: (String) -> Unit,
) {
    val colors = ClayTheme.colors
    val today = remember { LocalDate.now() }
    var selected by remember { mutableStateOf(today) }
    var editing by remember { mutableStateOf<ScheduleEditorTarget?>(null) }

    val occurrences = app.schedules.occurrences(selected)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = ClaySpacing.m, end = ClaySpacing.m, top = ClaySpacing.m, bottom = TAB_BAR_INSET,
            ),
            verticalArrangement = Arrangement.spacedBy(ClaySpacing.s),
        ) {
            item {
                ClayScreenHeader(
                    title = "일정",
                    subtitle = selected.format(dayFormatter),
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                ) {
                    // 오늘부터 2주. 더 먼 날짜는 이 화면에서 다루지 않는다 —
                    // 일정은 대개 이번 주 안의 일이다.
                    for (offset in 0..13) {
                        val date = today.plusDays(offset.toLong())
                        val isOn = date == selected
                        ClayChip(
                            text = if (offset == 0) "오늘" else date.format(chipFormatter),
                            tint = if (isOn) colors.accent else colors.surfaceRaised,
                            textColor = if (isOn) colors.textOnAccent else colors.textSecondary,
                            onClick = { selected = date },
                        )
                    }
                }
            }

            if (occurrences.isEmpty()) {
                item {
                    ClayEmptyState(
                        message = "이 날은 비어 있습니다",
                        detail = "아래 + 를 눌러 일정을 더하면, 그 시간에 어울리는 명언이 함께 옵니다.",
                        modifier = Modifier.padding(top = ClaySpacing.s),
                    )
                }
            }

            items(occurrences, key = { it.id }) { occurrence ->
                val quote = occurrence.resolvedQuote(app.quotes)
                ClayCard(onClick = { editing = ScheduleEditorTarget.Edit(occurrence.schedule) }) {
                    Column(
                        Modifier.fillMaxWidth().padding(ClaySpacing.m),
                        verticalArrangement = Arrangement.spacedBy(ClaySpacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                occurrence.start.format(timeFormatter),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.accent,
                            )
                            Text(
                                occurrence.displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = ClaySpacing.s),
                            )
                            if (occurrence.isRecurring) {
                                Icon(
                                    Icons.Filled.Repeat,
                                    contentDescription = "반복 일정",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        ClayChip(
                            text = occurrence.category.displayName,
                            tint = colors.tint(occurrence.category),
                            textColor = colors.textOnTint,
                        )

                        // 회차마다 다른 명언이 붙는다. 매일 같은 문장이면 이틀이면 읽지 않게 된다.
                        Text(
                            "“${quote.text}”",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = ClaySpacing.xs),
                        )
                        ClayChip(
                            text = "명언 보기",
                            onClick = { onOpenQuote(quote.slug) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = ScheduleEditorTarget.New(selected) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = ClaySpacing.m, bottom = TAB_BAR_INSET),
            containerColor = colors.accent,
            contentColor = colors.textOnAccent,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(ClayRadius.control),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "일정 더하기")
        }
    }

    editing?.let { target ->
        ScheduleEditorSheet(
            app = app,
            target = target,
            onDismiss = { editing = null },
        )
    }
}

/** 편집기를 무엇으로 열지. */
sealed interface ScheduleEditorTarget {
    data class New(val date: LocalDate) : ScheduleEditorTarget
    data class Edit(val schedule: Schedule) : ScheduleEditorTarget
}

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)
private val chipFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d(E)", Locale.KOREAN)
internal val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
